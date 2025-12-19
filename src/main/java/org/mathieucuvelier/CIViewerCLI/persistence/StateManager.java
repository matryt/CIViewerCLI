package org.mathieucuvelier.CIViewerCLI.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StateManager {
    private final DatabaseManager dbManager;

    private final PreparedStatement GET_LAST_TIMESTAMP_PS;
    private final PreparedStatement GET_RUNS_PS;
    private final PreparedStatement GET_JOB_DETAILS_PS;
    private final PreparedStatement GET_STEP_DETAILS_PS;
    private final PreparedStatement SAVE_TIMESTAMP_PS;
    private final PreparedStatement SAVE_RUN_PS;
    private final PreparedStatement SAVE_JOB_PS;
    private final PreparedStatement SAVE_STEP_DETAILS_PS;

    public StateManager(DatabaseManager dbManager) throws SQLException {
        this.dbManager = dbManager;

        var conn = dbManager.getConnection();

        this.GET_LAST_TIMESTAMP_PS = conn.prepareStatement(
                "SELECT last_check_timestamp FROM repo_state WHERE owner = ? AND repo = ?");
        this.GET_RUNS_PS = conn.prepareStatement(
                "SELECT run_id, status, conclusion FROM run_state WHERE owner = ? AND repo = ?");
        this.GET_JOB_DETAILS_PS = conn.prepareStatement(
                "SELECT job_id, status, conclusion FROM job_state WHERE owner = ? AND repo = ? AND run_id = ?");
        this.GET_STEP_DETAILS_PS = conn.prepareStatement(
                "SELECT step_name, status, conclusion FROM step_state WHERE owner = ? AND repo = ? AND run_id = ? AND job_id = ?"
        );
        this.SAVE_TIMESTAMP_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO repo_state (owner, repo, last_check_timestamp) VALUES (?, ?, ?)");
        this.SAVE_RUN_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO run_state (owner, repo, run_id, status, conclusion, last_updated) VALUES (?, ?, ?, ?, ?, ?)");
        this.SAVE_JOB_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO job_state (owner, repo, run_id, job_id, status, conclusion, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?)");
        this.SAVE_STEP_DETAILS_PS = conn.prepareStatement(
                "INSERT OR REPLACE INTO step_state (owner, repo, run_id, job_id, step_name, status, conclusion, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        );
    }

    public MonitorState loadState(String owner, String repo) {
        ZonedDateTime timestamp = loadLastCheckTimestamp(owner, repo);
        if (timestamp.equals(LocalDateTime.MIN.atZone(ZoneId.systemDefault()))) {
            return MonitorState.empty();
        }

        Map<Long, RunState> runs = loadRuns(owner, repo);
        return new MonitorState(timestamp, runs);
    }

    private ZonedDateTime loadLastCheckTimestamp(String owner, String repo) {
        try {
            GET_LAST_TIMESTAMP_PS.clearParameters();
            GET_LAST_TIMESTAMP_PS.setString(1, owner);
            GET_LAST_TIMESTAMP_PS.setString(2, repo);

            List<String> results = dbManager.preparedQuery(
                    GET_LAST_TIMESTAMP_PS,
                    rs -> {
                        try {
                            return rs.getString("last_check_timestamp");
                        } catch (SQLException e) {
                            return null;
                        }
                    });


            List<String> list = results.stream().filter(Objects::nonNull).toList();
            if (list.isEmpty()) return LocalDateTime.MIN.atZone(ZoneId.systemDefault());
            Instant instant = Instant.parse(list.getFirst());
            return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (SQLException e) {
            System.err.println("Error loading timestamp: " + e.getMessage());
            return ZonedDateTime.now();
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

            return runs.stream().filter(Objects::nonNull)
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
                    long jobId = rs.getLong("job_id");
                    Map<String, StepState> steps = loadSteps(owner, repo, runId, jobId);
                    return new JobState(
                            jobId,
                            rs.getString("status"),
                            rs.getString("conclusion"),
                            steps);
                } catch (SQLException e) {
                    return null;
                }
            });

            return jobs.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(JobState::jobId, j -> j));

        } catch (SQLException e) {
            System.err.println("Error loading jobs: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, StepState> loadSteps(String owner, String repo, long runId, long jobId) {
        try {
            GET_STEP_DETAILS_PS.clearParameters();
            GET_STEP_DETAILS_PS.setString(1, owner);
            GET_STEP_DETAILS_PS.setString(2, repo);
            GET_STEP_DETAILS_PS.setLong(3, runId);
            GET_STEP_DETAILS_PS.setLong(4, jobId);
            List<StepState> steps = dbManager.preparedQuery(GET_STEP_DETAILS_PS, rs -> {
                try {
                    return new StepState(
                            rs.getString("status"),
                            rs.getString("conclusion"),
                            rs.getString("step_name")
                    );
                }  catch (SQLException e) {
                    return null;
                }
            });
            Map<String, StepState> stepsMaps = new HashMap<>();
            for (StepState step : steps) {
                stepsMaps.put(
                        step.stepName(), step
                );
            }
            return stepsMaps;

        } catch (SQLException e) {
            System.err.println("Error loading steps: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void saveState(String owner, String repo, MonitorState state) {
        try {
            saveTimestamp(owner, repo, state.lastCheckTimestamp());
            for (RunState run : state.knownRuns().values()) {
                saveRun(owner, repo, run);
            }
        } catch (SQLException e) {
            System.err.println("Error saving state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveTimestamp(String owner, String repo, ZonedDateTime timestamp) throws SQLException {
        SAVE_TIMESTAMP_PS.clearParameters();
        SAVE_TIMESTAMP_PS.setString(1, owner);
        SAVE_TIMESTAMP_PS.setString(2, repo);
        SAVE_TIMESTAMP_PS.setString(3, timestamp.toInstant().toString());
        dbManager.executePreparedUpdate(SAVE_TIMESTAMP_PS);
    }

    private void saveRun(String owner, String repo, RunState run) throws SQLException {
        SAVE_RUN_PS.clearParameters();
        SAVE_RUN_PS.setString(1, owner);
        SAVE_RUN_PS.setString(2, repo);
        SAVE_RUN_PS.setLong(3, run.runId());
        SAVE_RUN_PS.setString(4, run.status());
        SAVE_RUN_PS.setString(5, run.conclusion());
        SAVE_RUN_PS.setLong(6, System.currentTimeMillis());
        dbManager.executePreparedUpdate(SAVE_RUN_PS);

        for (JobState job : run.knownJobs().values()) {
            saveJob(owner, repo, run.runId(), job);
        }
    }

    private void saveJob(String owner, String repo, long runId, JobState job) throws SQLException {
        SAVE_JOB_PS.clearParameters();
        SAVE_JOB_PS.setString(1, owner);
        SAVE_JOB_PS.setString(2, repo);
        SAVE_JOB_PS.setLong(3, runId);
        SAVE_JOB_PS.setLong(4, job.jobId());
        SAVE_JOB_PS.setString(5, job.status());
        SAVE_JOB_PS.setString(6, job.conclusion());
        SAVE_JOB_PS.setLong(7, System.currentTimeMillis());
        dbManager.executePreparedUpdate(SAVE_JOB_PS);

        for (StepState step : job.stepStates().values()) {
            saveStep(owner, repo, runId, job.jobId(), step);
        }
    }

    private void saveStep(String owner, String repo, long runId, long jobId, StepState step) throws SQLException {
        SAVE_STEP_DETAILS_PS.clearParameters();
        SAVE_STEP_DETAILS_PS.setString(1, owner);
        SAVE_STEP_DETAILS_PS.setString(2, repo);
        SAVE_STEP_DETAILS_PS.setLong(3, runId);
        SAVE_STEP_DETAILS_PS.setLong(4, jobId);
        SAVE_STEP_DETAILS_PS.setString(5, step.stepName());
        SAVE_STEP_DETAILS_PS.setString(6, step.status());
        SAVE_STEP_DETAILS_PS.setString(7, step.conclusion());
        SAVE_STEP_DETAILS_PS.setLong(8, System.currentTimeMillis());
        dbManager.executePreparedUpdate(SAVE_STEP_DETAILS_PS);
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
