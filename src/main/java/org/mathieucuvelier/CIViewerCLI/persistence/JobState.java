package org.mathieucuvelier.CIViewerCLI.persistence;

public record JobState(
    long jobId,
    String status,
    String conclusion
) {}
