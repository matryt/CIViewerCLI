package org.mathieucuvelier.CIViewerCLI.persistence;

import java.util.Map;

public record RunState(
    long runId,
    String status,
    String conclusion,
    Map<Long, JobState> knownJobs
) {
}