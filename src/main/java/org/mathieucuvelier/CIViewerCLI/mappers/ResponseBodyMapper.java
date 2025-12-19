package org.mathieucuvelier.CIViewerCLI.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResponseBodyMapper {
    private final ObjectMapper objectMapper;

    public ResponseBodyMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public WorkflowRunDTO deserializeWorkflowRun(JsonNode rootNode) {
        return objectMapper.convertValue(rootNode, WorkflowRunDTO.class);
    }

    public WorkflowJobDTO deserializeWorkflowJob(JsonNode rootNode) {
        return objectMapper.convertValue(rootNode, WorkflowJobDTO.class);
    }

    public List<WorkflowRunDTO> deserializeWorkflowRuns(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        if (!rootNode.has("workflow_runs")) throw new IllegalArgumentException("No workflow runs found");
        JsonNode runsNode = rootNode.get("workflow_runs");
        List<WorkflowRunDTO> workflowRuns = new ArrayList<>();
        if (runsNode.isArray()) {
            for (JsonNode runNode : runsNode) {
                WorkflowRunDTO workflowRun = deserializeWorkflowRun(runNode);
                workflowRuns.add(workflowRun);
            }
        }
        return workflowRuns;
    }

    public List<WorkflowJobDTO> deserializeWorkflowJobs(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        if (!rootNode.has("jobs")) throw new IllegalArgumentException("No workflow jobs found");
        JsonNode jobsNode = rootNode.get("jobs");
        List<WorkflowJobDTO> workflowJobs = new ArrayList<>();
        if (jobsNode.isArray()) {
            for (JsonNode jobNode : jobsNode) {
                WorkflowJobDTO workflowJob = deserializeWorkflowJob(jobNode);
                workflowJobs.add(workflowJob);
            }
        }
        return workflowJobs;
    }
}
