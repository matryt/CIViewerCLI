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
import org.mathieucuvelier.CIViewerCLI.utils.AnsiColors;

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
        
        Map<Long, JobState> jobsMap = jobs.stream()
            .collect(Collectors.toMap(
                WorkflowJobDTO::id,
                job -> new JobState(job.id(), job.status(), job.conclusion())
            ));
        
        RunState runState = new RunState(run.id(), run.status(), run.conclusion(), jobsMap);
        runsMap.put(run.id(), runState);
    }
    
    return new MonitorState(timestamp, runsMap);
}

    public void startMonitoring() {
        addHook();
        MonitorState state = initializeState();
        System.out.println("Starting WorkflowMonitor...");
        
        runMonitoringLoop(state);
        
        System.out.println("WorkflowMonitor stopped.");
    }

    private MonitorState initializeState() {
        MonitorState state = getState();
        
        if (state.lastCheckTimestamp() == 0) {
            return handleFirstRun();
        } else {
            System.out.println("Resuming from last check timestamp: " + 
                Instant.ofEpochMilli(state.lastCheckTimestamp()));
            lastCheckTimestamp = state.lastCheckTimestamp();
            return state;
        }
    }

    private MonitorState handleFirstRun() {
        System.out.println("First run for this repository - initializing state");
        lastCheckTimestamp = System.currentTimeMillis();
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = fetchRunsWithJobs(Instant.ofEpochMilli(0));
        
        MonitorState state = buildMonitorState(runsWithJobs, lastCheckTimestamp, Map.of());
        stateManager.saveState(owner, repo, state);
        
        System.out.println("State initialized. Monitoring for new events...");
        return state;
    }

    private void runMonitoringLoop(MonitorState state) {
        while (isRunning) {
            Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = 
                fetchRunsWithJobs(Instant.ofEpochMilli(lastCheckTimestamp));
            
            lastCheckTimestamp = System.currentTimeMillis();
            
            processAndDisplayEvents(runsWithJobs, state);
            
            state = updateAndSaveState(runsWithJobs, state);
            
            sleepBetweenPolls();
        }
    }

    private Map<WorkflowRunDTO, List<WorkflowJobDTO>> fetchRunsWithJobs(Instant since) {
        List<WorkflowRunDTO> runs = githubClient.getWorkflowRuns(since);
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = new HashMap<>();
        
        for (WorkflowRunDTO run : runs) {
            List<WorkflowJobDTO> jobs = githubClient.getWorkflowJobs(run.jobsUrl());
            runsWithJobs.put(run, jobs);
        }
        
        return runsWithJobs;
    }

    private void processAndDisplayEvents(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, 
                                         MonitorState state) {
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        if (events.isEmpty()) {
            System.out.println(AnsiColors.GRAY.colorize("No new events detected."));
        } else {
            for (Event event : events) {
                System.out.println(event.toFormattedString());
            }
        }
    }

    private MonitorState updateAndSaveState(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, 
                                            MonitorState currentState) {
        MonitorState newState = buildMonitorState(runsWithJobs, lastCheckTimestamp, currentState.knownRuns());
        stateManager.saveState(owner, repo, newState);
        return newState;
    }

    private void sleepBetweenPolls() {
        try {
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isRunning = false;
        }
    }
}
