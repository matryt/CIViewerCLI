package org.mathieucuvelier.CIViewerCLI.models;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.mathieucuvelier.CIViewerCLI.utils.AnsiColors;

public record Event(EventType type, ZonedDateTime timestamp, String workflowName, String jobName, String stepName,
                    String status, String conclusion, String branch, String commitSha) {
    
    private static final DateTimeFormatter FRIENDLY_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp.withZoneSameInstant(ZoneId.systemDefault()).format(FRIENDLY_FORMATTER)).append("] ");
        if (type == EventType.WORKFLOW_STARTED || type == EventType.JOB_STARTED || type == EventType.STEP_STARTED) {
            sb.append(AnsiColors.BLUE.colorize(type.toString()));
        } else if (type == EventType.STEP_FAILED) {
            sb.append(AnsiColors.RED.colorize(type.toString()));
        } else if (type == EventType.WORKFLOW_COMPLETED || type == EventType.JOB_COMPLETED ||  type == EventType.STEP_COMPLETED) {
            sb.append(AnsiColors.GREEN.colorize(type.toString()));
        } else {
            sb.append(type.toString());
        }
        sb.append(" - ");
        sb.append(workflowName);
        if (jobName != null) {
            sb.append(" / ").append(jobName);
        }
        if (stepName != null) {
            sb.append(" / ").append(stepName);
        }
        sb.append(" - ").append(status);
        if (conclusion != null) {
            sb.append(" (");
            if (conclusion.equals("success")) {
                sb.append(AnsiColors.GREEN.colorize(conclusion));
            }
            else if (conclusion.equals("failure") || conclusion.equals("failed")) {
                sb.append(AnsiColors.RED.colorize(conclusion));
            } else {
                sb.append(conclusion);
            }
            sb.append(")");
        }
        sb.append(" - ").append(branch).append("@").append(commitSha, 0, Math.min(7, commitSha.length()));
        return sb.toString();
    }

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
