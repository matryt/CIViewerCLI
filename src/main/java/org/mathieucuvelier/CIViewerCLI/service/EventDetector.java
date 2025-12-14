package org.mathieucuvelier.CIViewerCLI.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mathieucuvelier.CIViewerCLI.models.Event;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import org.mathieucuvelier.CIViewerCLI.persistence.JobState;
import org.mathieucuvelier.CIViewerCLI.persistence.MonitorState;
import org.mathieucuvelier.CIViewerCLI.persistence.RunState;

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
                events.add(Event.workflowStarted(run));
                events.addAll(detectJobs(run, jobs, Map.of()));
            }
        }
        return events;
    }

    private List<Event> detectJobs(WorkflowRunDTO run, List<WorkflowJobDTO> jobs, Map<Long, JobState> jobStates) {
        List<Event> events = new ArrayList<>();
        for (WorkflowJobDTO job: jobs) {
            if (jobStates.containsKey(job.id())) {
                if (job.completed(jobStates.get(job.id()))) events.add(Event.jobCompleted(run, job));
            }
            else {
                events.add(Event.jobStarted(run, job));
            }
        }
        return events;
    }
}
