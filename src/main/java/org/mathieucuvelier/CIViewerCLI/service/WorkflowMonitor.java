package org.mathieucuvelier.CIViewerCLI.service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mathieucuvelier.CIViewerCLI.models.Event;
import org.mathieucuvelier.CIViewerCLI.models.StepDto;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import org.mathieucuvelier.CIViewerCLI.persistence.*;
import org.mathieucuvelier.CIViewerCLI.utils.AnsiColors;

public class WorkflowMonitor {
    private final GithubClient githubClient;
    private ZonedDateTime lastDateTime; // Example timestamp
    private boolean isRunning = true;
    private final StateManager stateManager = new StateManager(new DatabaseManager());
    private final EventDetector detector = new EventDetector();
    private final String owner;
    private final String repo;
    private int pollCount = 0;
    
    private long startTime;
    private int workflowsStarted = 0;
    private int workflowsCompleted = 0;
    private int jobsStarted = 0;
    private int jobsCompleted = 0;
    private int stepsStarted = 0;
    private int stepsCompleted = 0;
    private int stepsFailed = 0;
    
    public WorkflowMonitor(GithubClient githubClient, String owner, String repo) throws SQLException, IOException {
        this.githubClient = githubClient;
        this.owner = owner;
        this.repo = repo;        
    }

    private void addHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lastDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
            isRunning = false;
            System.out.println("\n" + AnsiColors.GRAY.colorize("Shutting down gracefully..."));
            displaySummary();
        }));
        Runtime.getRuntime().addShutdownHook(
            new Thread(stateManager::close)
        );
    }

    private MonitorState getState() {
        return stateManager.loadState(owner, repo);
    }

    private MonitorState buildMonitorState(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, ZonedDateTime datetime, Map<Long, RunState> knownRuns) {
    Map<Long, RunState> runsMap = new HashMap<>(knownRuns);
    
    for (Map.Entry<WorkflowRunDTO, List<WorkflowJobDTO>> entry : runsWithJobs.entrySet()) {
        WorkflowRunDTO run = entry.getKey();
        List<WorkflowJobDTO> jobs = entry.getValue();

        Map<Long, JobState> jobsMap = jobs.stream()
            .collect(Collectors.toMap(
                WorkflowJobDTO::id,
                job -> {
                    Map<String, StepState> steps = new HashMap<>();
                    for (StepDto stepDto : job.steps()) {
                        steps.put(stepDto.name(), new StepState(stepDto.status(), stepDto.conclusion(), stepDto.name()));
                    }
                    return new JobState(job.id(), job.status(), job.conclusion(), steps);
                }

                )
            );
        
        RunState runState = new RunState(run.id(), run.status(), run.conclusion(), jobsMap);
        runsMap.put(run.id(), runState);
    }
    
    return new MonitorState(datetime, runsMap);
}

    public void startMonitoring() {
        startTime = System.currentTimeMillis();
        addHook();
        MonitorState state = initializeState();
        System.out.println("Starting WorkflowMonitor...");
        
        runMonitoringLoop(state);
        
        System.out.println("WorkflowMonitor stopped.");
    }

    private MonitorState initializeState() {
        MonitorState state = getState();
        
        if (state.lastCheckTimestamp().equals(LocalDateTime.MIN.atZone(ZoneId.systemDefault()))) {
            return handleFirstRun();
        } else {
            DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            System.out.println("Resuming from last check timestamp: " +
                    state.lastCheckTimestamp().format(formatter));
            lastDateTime = state.lastCheckTimestamp();
            return state;
        }
    }

    private MonitorState handleFirstRun() {
        System.out.println("First run for this repository - initializing state");
        lastDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = fetchRunsWithJobs(LocalDateTime.MIN.atZone(ZoneId.systemDefault()));
        
        MonitorState state = buildMonitorState(runsWithJobs, lastDateTime, Map.of());
        stateManager.saveState(owner, repo, state);
        
        System.out.println("State initialized. Monitoring for new events...");
        return state;
    }

    private void runMonitoringLoop(MonitorState state) {
        while (isRunning) {
            Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = 
                fetchRunsWithJobs(lastDateTime);

            lastDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
            
            processAndDisplayEvents(runsWithJobs, state);
            
            state = updateAndSaveState(runsWithJobs, state);
            
            sleepBetweenPolls();
        }
    }

    private Map<WorkflowRunDTO, List<WorkflowJobDTO>> fetchRunsWithJobs(ZonedDateTime datetime) {
        List<WorkflowRunDTO> runs = githubClient.getWorkflowRuns(datetime);
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
            pollCount++;
            System.out.println(AnsiColors.GRAY.colorize("Monitoring... (" + pollCount + " polls, no events)"));
        } else {
            pollCount = 0;
            for (Event event : events) {
                trackEvent(event);
                System.out.println(event.toFormattedString());
            }
        }
    }

    private MonitorState updateAndSaveState(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, 
                                            MonitorState currentState) {
        MonitorState newState = buildMonitorState(runsWithJobs, lastDateTime, currentState.knownRuns());
        stateManager.saveState(owner, repo, newState);
        return newState;
    }

    private void sleepBetweenPolls() {
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isRunning = false;
        }
    }
    
    private void trackEvent(Event event) {
        switch (event.type()) {
            case WORKFLOW_STARTED -> workflowsStarted++;
            case WORKFLOW_COMPLETED -> workflowsCompleted++;
            case JOB_STARTED -> jobsStarted++;
            case JOB_COMPLETED -> jobsCompleted++;
            case STEP_STARTED -> stepsStarted++;
            case STEP_COMPLETED -> stepsCompleted++;
            case STEP_FAILED -> stepsFailed++;
        }
    }
    
    private void displaySummary() {
        long durationMs = System.currentTimeMillis() - startTime;
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        String duration;
        if (hours > 0) {
            duration = String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            duration = String.format("%dm %ds", minutes, seconds % 60);
        } else {
            duration = String.format("%ds", seconds);
        }
        
        int totalEvents = workflowsStarted + workflowsCompleted + jobsStarted + jobsCompleted + 
                          stepsStarted + stepsCompleted + stepsFailed;
        
        System.out.println("\n" + AnsiColors.BLUE.colorize("=== Monitoring Summary ==="));
        System.out.println("Duration: " + duration);
        System.out.println("Events detected: " + totalEvents);
        if (totalEvents > 0) {
            System.out.println("  - Workflows: " + workflowsStarted + " started, " + workflowsCompleted + " completed");
            System.out.println("  - Jobs: " + jobsStarted + " started, " + jobsCompleted + " completed");
            System.out.println("  - Steps: " + stepsStarted + " started, " + stepsCompleted + " completed, " + 
                             AnsiColors.RED.colorize(String.valueOf(stepsFailed)) + " failed");
        }
        System.out.println("\n" + AnsiColors.GRAY.colorize("Final state saved."));
    }
}
