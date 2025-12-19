package org.mathieucuvelier.CIViewerCLI.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseBodyMapperTest {

    private ResponseBodyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ResponseBodyMapper();
    }

    @Test
    void testDeserializeWorkflowRun_ValidJson() throws IOException {
        String json = "{\"id\": 1, \"name\": \"Test Run\"}";
        JsonNode rootNode = new ObjectMapper().readTree(json);

        WorkflowRunDTO result = mapper.deserializeWorkflowRun(rootNode);

        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("Test Run", result.name());
    }

    @Test
    void testDeserializeWorkflowJob_ValidJson() throws IOException {
        String json = "{\"id\": 2, \"name\": \"Test Job\"}";
        JsonNode rootNode = new ObjectMapper().readTree(json);

        WorkflowJobDTO result = mapper.deserializeWorkflowJob(rootNode);

        assertNotNull(result);
        assertEquals(2, result.id());
        assertEquals("Test Job", result.name());
    }

    @Test
    void testDeserializeWorkflowRuns_ValidJson() throws IOException {
        String json = "{\"workflow_runs\": [{\"id\": 1, \"name\": \"Run 1\"}, {\"id\": 2, \"name\": \"Run 2\"}]}";

        List<WorkflowRunDTO> result = mapper.deserializeWorkflowRuns(json);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Run 1", result.get(0).name());
        assertEquals("Run 2", result.get(1).name());
    }

    @Test
    void testDeserializeWorkflowJobs_ValidJson() throws IOException {
        String json = "{\"jobs\": [{\"id\": 1, \"name\": \"Job 1\"}, {\"id\": 2, \"name\": \"Job 2\"}]}";

        List<WorkflowJobDTO> result = mapper.deserializeWorkflowJobs(json);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Job 1", result.get(0).name());
        assertEquals("Job 2", result.get(1).name());
    }

    @Test
    void testDeserializeWorkflowRuns_InvalidJson() {
        String invalidJson = "{\"invalid\": \"data\"}";

        assertThrows(IllegalArgumentException.class, () -> mapper.deserializeWorkflowRuns(invalidJson));
    }

    @Test
    void testDeserializeWorkflowJobs_InvalidJson() {
        String invalidJson = "{\"invalid\": \"data\"}";

        assertThrows(IllegalArgumentException.class, () -> mapper.deserializeWorkflowJobs(invalidJson));
    }
}