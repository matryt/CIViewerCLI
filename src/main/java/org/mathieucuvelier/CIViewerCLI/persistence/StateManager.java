package org.mathieucuvelier.CIViewerCLI.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StateManager {
    private final DatabaseManager dbManager;

    private final PreparedStatement GET_LAST_TIMESTAMP_PS;
    private final PreparedStatement GET_RUNS_PS;
    private final PreparedStatement GET_JOB_DETAILS_PS;
    private final PreparedStatement SAVE_TIMESTAMP_PS;
    private final PreparedStatement SAVE_RUN_PS;
    private final PreparedStatement SAVE_JOB_PS;

    public StateManager(DatabaseManager dbManager) throws SQLException {
        this.dbManager = dbManager;

        var conn = dbManager.getConnection();

        this.GET_LAST_TIMESTAMP_PS = conn.prepareStatement(
                "SELECT last_check_timestamp FROM repo_state WHERE owner = ? AND repo = ?");
        this.GET_RUNS_PS = conn.prepareStatement(
                "SELECT run_id, status, conclusion FROM run_state WHERE owner = ? AND repo = ?");
        this.GET_JOB_DETAILS_PS = conn.prepareStatement(
                "SELECT job_id, status, conclusion FROM job_state WHERE owner = ? AND repo = ? AND run_id = ?");
        this.SAVE_TIMESTAMP_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO repo_state (owner, repo, last_check_timestamp) VALUES (?, ?, ?)");
        this.SAVE_RUN_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO run_state (owner, repo, run_id, status, conclusion, last_updated) VALUES (?, ?, ?, ?, ?, ?)");
        this.SAVE_JOB_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO job_state (owner, repo, run_id, job_id, status, conclusion, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?)");
    }

    public MonitorState loadState(String owner, String repo) {
        long timestamp = loadLastCheckTimestamp(owner, repo);
        if (timestamp == 0) {
            return MonitorState.empty();
        }

        Map<Long, RunState> runs = loadRuns(owner, repo);
        return new MonitorState(timestamp, runs);
    }

    private long loadLastCheckTimestamp(String owner, String repo) {
        try {
            GET_LAST_TIMESTAMP_PS.clearParameters();
            GET_LAST_TIMESTAMP_PS.setString(1, owner);
            GET_LAST_TIMESTAMP_PS.setString(2, repo);

            List<Long> results = dbManager.preparedQuery(
                    GET_LAST_TIMESTAMP_PS,
                    rs -> {
                        try {
                            return rs.getLong("last_check_timestamp");
                        } catch (SQLException e) {
                            return null;
                        }
                    });

            return results.stream().filter(t -> t != null).toList().isEmpty() ? 0L : results.get(0);
        } catch (SQLException e) {
            System.err.println("Error loading timestamp: " + e.getMessage());
            return 0L;
        }
    }

    private Map<Long, RunState> loadRuns(String owner, String repo) {
        try {
            GET_RUNS_PS.clearParameters();
            GET_RUNS_PS.setString(1, owner);
            GET_RUNS_PS.setString(2, repo);

            List<RunState> runs = dbManager.preparedQuery(GET_RUNS_PS, rs -> {
                try {
                    long runId = rs.getLong("run_id");
                    String status = rs.getString("status");
                    String conclusion = rs.getString("conclusion");
                    Map<Long, JobState> jobs = loadJobs(owner, repo, runId);
                    return new RunState(runId, status, conclusion, jobs);
                } catch (SQLException e) {
                    return null;
                }
            });

            return runs.stream().filter(r -> r != null)
                    .collect(Collectors.toMap(RunState::runId, r -> r));

        } catch (SQLException e) {
            System.err.println("Error loading runs: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, JobState> loadJobs(String owner, String repo, long runId) {
        try {
            GET_JOB_DETAILS_PS.clearParameters();
            GET_JOB_DETAILS_PS.setString(1, owner);
            GET_JOB_DETAILS_PS.setString(2, repo);
            GET_JOB_DETAILS_PS.setLong(3, runId);

            List<JobState> jobs = dbManager.preparedQuery(GET_JOB_DETAILS_PS, rs -> {
                try {
                    return new JobState(
                            rs.getLong("job_id"),
                            rs.getString("status"),
                            rs.getString("conclusion"));
                } catch (SQLException e) {
                    return null;
                }
            });

            return jobs.stream().filter(j -> j != null)
                    .collect(Collectors.toMap(JobState::jobId, j -> j));

        } catch (SQLException e) {
            System.err.println("Error loading jobs: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void saveState(String owner, String repo, MonitorState state) {
        try {
            SAVE_TIMESTAMP_PS.clearParameters();
            SAVE_TIMESTAMP_PS.setString(1, owner);
            SAVE_TIMESTAMP_PS.setString(2, repo);
            SAVE_TIMESTAMP_PS.setLong(3, state.lastCheckTimestamp());
            dbManager.executePreparedUpdate(SAVE_TIMESTAMP_PS);

            for (RunState run : state.knownRuns().values()) {
                SAVE_RUN_PS.clearParameters();
                SAVE_RUN_PS.setString(1, owner);
                SAVE_RUN_PS.setString(2, repo);
                SAVE_RUN_PS.setLong(3, run.runId());
                SAVE_RUN_PS.setString(4, run.status());
                SAVE_RUN_PS.setString(5, run.conclusion());
                SAVE_RUN_PS.setLong(6, System.currentTimeMillis());
                dbManager.executePreparedUpdate(SAVE_RUN_PS);

                for (JobState job : run.knownJobs().values()) {
                    SAVE_JOB_PS.clearParameters();
                    SAVE_JOB_PS.setString(1, owner);
                    SAVE_JOB_PS.setString(2, repo);
                    SAVE_JOB_PS.setLong(3, run.runId());
                    SAVE_JOB_PS.setLong(4, job.jobId());
                    SAVE_JOB_PS.setString(5, job.status());
                    SAVE_JOB_PS.setString(6, job.conclusion());
                    SAVE_JOB_PS.setLong(7, System.currentTimeMillis());
                    dbManager.executePreparedUpdate(SAVE_JOB_PS);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            GET_LAST_TIMESTAMP_PS.close();
            GET_RUNS_PS.close();
            GET_JOB_DETAILS_PS.close();
            SAVE_TIMESTAMP_PS.close();
            SAVE_RUN_PS.close();
            SAVE_JOB_PS.close();
            dbManager.close();
        } catch (SQLException e) {
            System.err.println("Error closing StateManager: " + e.getMessage());
        }
    }
}
