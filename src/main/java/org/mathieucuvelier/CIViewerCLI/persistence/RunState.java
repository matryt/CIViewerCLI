package org.mathieucuvelier.CIViewerCLI.persistence;

import java.util.Map;

public record RunState(
    long runId,
    String status,
    String conclusion,
    Map<Long, JobState> knownJobs
) {
    public boolean hasSameStatusAndConclusion(String wantedStatus, String wantedConclusion) {
        return wantedStatus.equals(status) && wantedConclusion.equals(conclusion);
    }
}