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
            if (previousState.knownRuns().containsKey(run.id())) {
                RunState state = previousState.knownRuns().get(run.id());
                if (run.status().equals("completed") && !state.status().equals("completed")) {
                    events.add(Event.workflowCompleted(run));
                }
                events.addAll(detectJobs(run, jobs, state.knownJobs()));

            }
            else {
                if (!run.status().equals("completed")) {
                    events.add(Event.workflowStarted(run));
                    events.addAll(detectJobs(run, jobs, Map.of()));
                } else {
                    events.add(Event.workflowCompleted(run));
                    for (WorkflowJobDTO job : jobs) {
                        if (job.status().equals("completed")) {
                            events.add(Event.jobCompleted(run, job));
                        }
                    }
                }
            }
        }
        return events;
    }

    private List<Event> detectJobs(WorkflowRunDTO run, List<WorkflowJobDTO> jobs, Map<Long, JobState> jobStates) {
        List<Event> events = new ArrayList<>();
        for (WorkflowJobDTO job: jobs) {
            if (jobStates.containsKey(job.id())) {
                JobState state = jobStates.get(job.id());
                if (job.completed(state)) events.add(Event.jobCompleted(run, job));
                events.addAll(detectSteps(run, job, state.stepStates()));
            }
            else {
                events.add(Event.jobStarted(run, job));
                events.addAll(detectSteps(run, job, Map.of()));
            }
        }
        return events;
    }

    private List<Event> detectSteps(WorkflowRunDTO run, WorkflowJobDTO job, Map<String, StepState> stepStates) {
        List<Event> events = new ArrayList<>();
        for (StepDto step: job.steps()) {
            if (!stepStates.containsKey(step.name()) || !step.status().equals(stepStates.get(step.name()).status())) {
                switch (step.status()) {
                    case "completed" -> events.add(Event.stepCompleted(run, job, step));
                    case "failure", "failed" -> events.add(Event.stepFailed(run, job, step));
                    case "started" -> events.add(Event.stepStarted(run, job, step));
                }
            }
        }
        return events;
    }
}
