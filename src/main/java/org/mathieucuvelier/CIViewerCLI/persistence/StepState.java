package org.mathieucuvelier.CIViewerCLI.persistence;

public record StepState(
        String status,
        String conclusion,
        String stepName
) {}
