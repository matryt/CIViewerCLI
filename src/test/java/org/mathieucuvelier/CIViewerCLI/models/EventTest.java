package org.mathieucuvelier.CIViewerCLI.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class EventTest {
    
    private WorkflowRunDTO createTestRun(String status, String conclusion) {
        return new WorkflowRunDTO(
            123L,
            "Test Workflow",
            "main",
            "abc123",
            status,
            conclusion,
            LocalDateTime.of(2025, 12, 19, 10, 30, 0),
            LocalDateTime.of(2025, 12, 19, 10, 35, 0),
            "https://api.github.com/repos/owner/repo/actions/runs/123/jobs"
        );
    }
    
    private WorkflowJobDTO createTestJob(String status, String conclusion) {
        return new WorkflowJobDTO(
            456L,
            "Build Job",
            status,
            conclusion,
            "2025-12-19T10:30:00Z",
            "2025-12-19T10:35:00Z",
            List.of()
        );
    }
    
    private StepDto createTestStep(String status, String conclusion) {
        return new StepDto(
            "Build Step",
            status,
            conclusion,
            1,
            "2025-12-19T10:30:00Z",
            "2025-12-19T10:35:00Z"
        );
    }
    
    @Test
    void testWorkflowStarted() {
        WorkflowRunDTO run = createTestRun("queued", null);
        Event event = Event.workflowStarted(run);
        
        assertEquals(EventType.WORKFLOW_STARTED, event.type());
        assertNotNull(event.timestamp());
        assertTrue(event.toFormattedString().contains("WORKFLOW_STARTED"));
        assertTrue(event.toFormattedString().contains("Test Workflow"));
    }
    
    @Test
    void testWorkflowCompleted() {
        WorkflowRunDTO run = createTestRun("completed", "success");
        Event event = Event.workflowCompleted(run);
        
        assertEquals(EventType.WORKFLOW_COMPLETED, event.type());
        assertTrue(event.toFormattedString().contains("WORKFLOW_COMPLETED"));
        assertTrue(event.toFormattedString().contains("success"));
    }
    
    @Test
    void testJobStarted() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("queued", null);
        Event event = Event.jobStarted(run, job);
        
        assertEquals(EventType.JOB_STARTED, event.type());
        assertTrue(event.toFormattedString().contains("JOB_STARTED"));
        assertTrue(event.toFormattedString().contains("Build Job"));
    }
    
    @Test
    void testJobCompleted() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("completed", "success");
        Event event = Event.jobCompleted(run, job);
        
        assertEquals(EventType.JOB_COMPLETED, event.type());
        assertTrue(event.toFormattedString().contains("JOB_COMPLETED"));
        assertTrue(event.toFormattedString().contains("success"));
    }
    
    @Test
    void testStepStarted() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("in_progress", null);
        StepDto step = createTestStep("in_progress", null);
        Event event = Event.stepStarted(run, job, step);
        
        assertEquals(EventType.STEP_STARTED, event.type());
        assertTrue(event.toFormattedString().contains("STEP_STARTED"));
        assertTrue(event.toFormattedString().contains("Build Step"));
    }
    
    @Test
    void testStepCompleted() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("in_progress", null);
        StepDto step = createTestStep("completed", "success");
        Event event = Event.stepCompleted(run, job, step);
        
        assertEquals(EventType.STEP_COMPLETED, event.type());
        assertTrue(event.toFormattedString().contains("STEP_COMPLETED"));
        assertTrue(event.toFormattedString().contains("Build Step"));
    }
    
    @Test
    void testStepFailed() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("in_progress", null);
        StepDto step = createTestStep("completed", "failure");
        Event event = Event.stepFailed(run, job, step);
        
        assertEquals(EventType.STEP_FAILED, event.type());
        assertTrue(event.toFormattedString().contains("STEP_FAILED"));
        assertTrue(event.toFormattedString().contains("Build Step"));
        // Should be in red (ANSI color)
        assertTrue(event.toFormattedString().contains("\u001B[31m"));
    }
    
    @Test
    void testFormattedStringContainsTimestamp() {
        WorkflowRunDTO run = createTestRun("queued", null);
        Event event = Event.workflowStarted(run);
        
        String formatted = event.toFormattedString();
        // Should contain date-time format: yyyy-MM-dd HH:mm:ss z
        assertTrue(formatted.matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }
    
    @Test
    void testFormattedStringContainsWorkflowInfo() {
        WorkflowRunDTO run = createTestRun("completed", "success");
        Event event = Event.workflowCompleted(run);
        
        String formatted = event.toFormattedString();
        assertTrue(formatted.contains("Test Workflow"));
        assertTrue(formatted.contains("123"));
    }
    
    @Test
    void testFormattedStringContainsJobInfo() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("completed", "success");
        Event event = Event.jobCompleted(run, job);
        
        String formatted = event.toFormattedString();
        assertTrue(formatted.contains("Build Job"));
    }
    
    @Test
    void testFormattedStringContainsStepInfo() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("in_progress", null);
        StepDto step = createTestStep("completed", "success");
        Event event = Event.stepCompleted(run, job, step);
        
        String formatted = event.toFormattedString();
        assertTrue(formatted.contains("Build Step"));
    }
    
    @Test
    void testTimestampInUTC() {
        WorkflowRunDTO run = createTestRun("queued", null);
        Event event = Event.workflowStarted(run);
        
        assertEquals(ZoneId.of("UTC"), event.timestamp().getZone());
    }
    
    @Test
    void testWorkflowCompletedWithFailure() {
        WorkflowRunDTO run = createTestRun("completed", "failure");
        Event event = Event.workflowCompleted(run);
        
        String formatted = event.toFormattedString();
        assertTrue(formatted.contains("failure"));
        // Should be in red
        assertTrue(formatted.contains("\u001B[31m"));
    }
    
    @Test
    void testJobCompletedWithCancelled() {
        WorkflowRunDTO run = createTestRun("in_progress", null);
        WorkflowJobDTO job = createTestJob("completed", "cancelled");
        Event event = Event.jobCompleted(run, job);
        
        String formatted = event.toFormattedString();
        assertTrue(formatted.contains("cancelled"));
    }
}
