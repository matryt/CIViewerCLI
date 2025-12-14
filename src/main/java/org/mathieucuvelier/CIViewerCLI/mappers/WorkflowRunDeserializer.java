package org.mathieucuvelier.CIViewerCLI.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowRunDeserializer {
    private final ObjectMapper objectMapper;

    public WorkflowRunDeserializer() {
        objectMapper = new ObjectMapper();
    }
}
