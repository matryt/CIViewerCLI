package org.mathieucuvelier.CIViewerCLI.models;

import java.time.Instant;

import org.mathieucuvelier.CIViewerCLI.persistence.RunState;

public record Event(EventType type, Instant timestamp, String workflowName, String jobName, String stepName,
        String status, String conclusion, String branch, String commitSha) {
    
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp.toString()).append("] ");
        sb.append(type).append(" - ");
        sb.append(workflowName);
        if (jobName != null) {
            sb.append(" / ").append(jobName);
        }
        if (stepName != null) {
            sb.append(" / ").append(stepName);
        }
        sb.append(" - ").append(status);
        if (conclusion != null) {
            sb.append(" (").append(conclusion).append(")");
        }
        sb.append(" - ").append(branch).append("@").append(commitSha.substring(0, Math.min(7, commitSha.length())));
        return sb.toString();
    }

    // Factory methods pour les workflows
    public static Event workflowStarted(WorkflowRunDTO run) {
        return new Event(
            EventType.WORKFLOW_STARTED,
            run.getTimestampAsInstant(),
            run.name(),
            null, 
            null,
            run.status(),
            run.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    public static Event workflowCompleted(WorkflowRunDTO run) {
        return new Event(
            EventType.WORKFLOW_COMPLETED,
            run.getTimestampAsInstant(),
            run.name(),
            null,
            null,
            run.status(),
            run.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    // Factory methods pour les jobs
    public static Event jobStarted(WorkflowRunDTO run, WorkflowJobDTO job) {
        return new Event(
            EventType.JOB_STARTED,
            run.getTimestampAsInstant(),
            run.name(),
            job.name(),
            null,
            job.status(),
            job.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    public static Event jobCompleted(WorkflowRunDTO run, WorkflowJobDTO job) {
        return new Event(
            EventType.JOB_COMPLETED,
            run.getTimestampAsInstant(),
            run.name(),
            job.name(),
            null,
            job.status(),
            job.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    // Factory methods pour les steps
    public static Event stepStarted(WorkflowRunDTO run, WorkflowJobDTO job, StepDto step) {
        return new Event(
            EventType.STEP_STARTED,
            run.getTimestampAsInstant(),
            run.name(),
            job.name(),
            step.name(),
            step.status(),
            step.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    public static Event stepCompleted(WorkflowRunDTO run, WorkflowJobDTO job, StepDto step) {
        return new Event(
            EventType.STEP_COMPLETED,
            run.getTimestampAsInstant(),
            run.name(),
            job.name(),
            step.name(),
            step.status(),
            step.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }

    public static Event stepFailed(WorkflowRunDTO run, WorkflowJobDTO job, StepDto step) {
        return new Event(
            EventType.STEP_FAILED,
            run.getTimestampAsInstant(),
            run.name(),
            job.name(),
            step.name(),
            step.status(),
            step.conclusion(),
            run.headBranch(),
            run.headSha()
        );
    }
}
