package org.mathieucuvelier.CIViewerCLI.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mathieucuvelier.CIViewerCLI.models.Event;
import org.mathieucuvelier.CIViewerCLI.models.StepDto;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import org.mathieucuvelier.CIViewerCLI.persistence.JobState;
import org.mathieucuvelier.CIViewerCLI.persistence.MonitorState;
import org.mathieucuvelier.CIViewerCLI.persistence.RunState;
import org.mathieucuvelier.CIViewerCLI.persistence.StepState;

public class EventDetector {
    public List<Event> detectEvents(Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs, MonitorState previousState) {
        List<Event> events = new ArrayList<>();
        for (Map.Entry<WorkflowRunDTO, List<WorkflowJobDTO>> runEntry: runsWithJobs.entrySet()) {
            WorkflowRunDTO run = runEntry.getKey();
            List<WorkflowJobDTO> jobs = runEntry.getValue();
            
            events.addAll(detectWorkflowEvents(run, previousState));
            
            Map<Long, JobState> previousJobs = previousState.knownRuns().containsKey(run.id())
                ? previousState.knownRuns().get(run.id()).knownJobs()
                : Map.of();
            
            events.addAll(detectJobs(run, jobs, previousJobs));
        }
        return events;
    }

    private List<Event> detectWorkflowEvents(WorkflowRunDTO run, MonitorState previousState) {
        List<Event> events = new ArrayList<>();
        if (previousState.knownRuns().containsKey(run.id())) {
            if (run.status().equals("completed")) {
                events.add(Event.workflowCompleted(run));
            } else if ("in_progress".equals(run.status()) || "queued".equals(run.status())) {
                events.add(Event.workflowStarted(run));
            }
        } else {
            RunState previousRunState = previousState.knownRuns().get(run.id());
            boolean statusChanged = !run.status().equals(previousRunState.status());
            if (statusChanged && run.status().equals("completed")) {
                events.add(Event.workflowCompleted(run));
            }
        }
        
        return events;
    }

    private List<Event> detectJobs(WorkflowRunDTO run, List<WorkflowJobDTO> jobs, Map<Long, JobState> jobStates) {
        List<Event> events = new ArrayList<>();
        for (WorkflowJobDTO job : jobs) {
            JobState previousJobState = jobStates.get(job.id());

            if (previousJobState == null) {
                if (job.status().equals("completed")) {
                    events.add(Event.jobCompleted(run, job));
                } else if ("in_progress".equals(job.status()) || "queued".equals(job.status())) {
                    events.add(Event.jobStarted(run, job));
                }
            } else {
                boolean statusChanged = !job.status().equals(previousJobState.status());
                if (statusChanged && job.status().equals("completed")) {
                    events.add(Event.jobCompleted(run, job));
                }
            }
            
            Map<String, StepState> previousSteps = previousJobState != null 
                ? previousJobState.stepStates() 
                : Map.of();
            events.addAll(detectSteps(run, job, previousSteps));
        }
        return events;
    }

    private List<Event> detectSteps(WorkflowRunDTO run, WorkflowJobDTO job, Map<String, StepState> stepStates) {
        List<Event> events = new ArrayList<>();
        for (StepDto step : job.steps()) {
            if (stepStates.containsKey(step.name())) {
                if (step.status().equals("completed")) {
                    if ("failure".equals(step.conclusion())) {
                        events.add(Event.stepFailed(run, job, step));
                    } else {
                        events.add(Event.stepCompleted(run, job, step));
                    }
                } else if ("in_progress".equals(step.status()) || "queued".equals(step.status())) {
                    events.add(Event.stepStarted(run, job, step));
                }
            } else {
                StepState previousState = stepStates.get(step.name());
                boolean statusChanged = !step.status().equals(previousState.status());
                
                if (statusChanged && step.status().equals("completed")) {
                    if ("failure".equals(step.conclusion())) {
                        events.add(Event.stepFailed(run, job, step));
                    } else {
                        events.add(Event.stepCompleted(run, job, step));
                    }
                }
            }
        }
        return events;
    }
}
