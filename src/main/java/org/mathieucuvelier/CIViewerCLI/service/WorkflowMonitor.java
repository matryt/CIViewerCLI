package org.mathieucuvelier.CIViewerCLI.service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mathieucuvelier.CIViewerCLI.models.Event;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import org.mathieucuvelier.CIViewerCLI.persistence.DatabaseManager;
import org.mathieucuvelier.CIViewerCLI.persistence.JobState;
import org.mathieucuvelier.CIViewerCLI.persistence.MonitorState;
import org.mathieucuvelier.CIViewerCLI.persistence.RunState;
import org.mathieucuvelier.CIViewerCLI.persistence.StateManager;

public class WorkflowMonitor {
    private final GithubClient githubClient;
    private long lastCheckTimestamp; // Example timestamp
    private boolean isRunning = true;
    private final StateManager stateManager = new StateManager(new DatabaseManager());
    private final EventDetector detector = new EventDetector();
    private final String owner;
    private final String repo;
    
    public WorkflowMonitor(GithubClient githubClient, String owner, String repo) throws SQLException, IOException {
        this.githubClient = githubClient;
        this.owner = owner;
        this.repo = repo;        
    }

    private void addHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lastCheckTimestamp = System.currentTimeMillis();
            isRunning = false;
            System.out.println("Shutting down WorkflowMonitor...");
        }));
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                stateManager.close();
            })
        );
    }

    private MonitorState getState() {
        return stateManager.loadState(owner, repo);
    }

    private MonitorState buildMonitorState(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, long timestamp, Map<Long, RunState> knownRuns) {
    Map<Long, RunState> runsMap = new HashMap<>(knownRuns);
    
    for (Map.Entry<WorkflowRunDTO, List<WorkflowJobDTO>> entry : runsWithJobs.entrySet()) {
        WorkflowRunDTO run = entry.getKey();
        List<WorkflowJobDTO> jobs = entry.getValue();
        
        // Construire la map des jobs
        Map<Long, JobState> jobsMap = jobs.stream()
            .collect(Collectors.toMap(
                WorkflowJobDTO::id,
                job -> new JobState(job.id(), job.status(), job.conclusion())
            ));
        
        // Créer le RunState
        RunState runState = new RunState(run.id(), run.status(), run.conclusion(), jobsMap);
        runsMap.put(run.id(), runState);
    }
    
    return new MonitorState(timestamp, runsMap);
}

    public void startMonitoring() {
        addHook();
        MonitorState state = getState();
        System.out.println("Starting WorkflowMonitor...");
        lastCheckTimestamp = state.lastCheckTimestamp();
        if (lastCheckTimestamp > 0) {
            System.out.println("Resuming from last check timestamp: " + lastCheckTimestamp);
            System.out.println(githubClient.getWorkflowRuns(Instant.ofEpochMilli(lastCheckTimestamp)));
        }
        while (isRunning) {
            List<WorkflowRunDTO> newRuns = githubClient.getWorkflowRuns(Instant.ofEpochMilli(lastCheckTimestamp));
            Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = new HashMap<>();

            for (WorkflowRunDTO run : newRuns) {
                List<WorkflowJobDTO> jobs = githubClient.getWorkflowJobs(run.jobsUrl());
                runsWithJobs.put(run, jobs);
            }
            lastCheckTimestamp = System.currentTimeMillis();
            
            for (Event event: detector.detectEvents(runsWithJobs, state)) {
                System.out.println(event.toFormattedString());
            }

            MonitorState newState = buildMonitorState(runsWithJobs, lastCheckTimestamp, state.knownRuns());
            stateManager.saveState(owner, repo, newState);
            state = newState;

            try {
                Thread.sleep(5000); // Sleep for 5 seconds before next check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("WorkflowMonitor stopped.");
    }
}
