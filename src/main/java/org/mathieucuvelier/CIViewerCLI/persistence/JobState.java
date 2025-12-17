package org.mathieucuvelier.CIViewerCLI.persistence;

import java.util.Map;

public record JobState(
    long jobId,
    String status,
    String conclusion,
    Map<String, StepState> stepStates
) {}
