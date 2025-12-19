package org.mathieucuvelier.CIViewerCLI.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.mathieucuvelier.CIViewerCLI.models.*;
import org.mathieucuvelier.CIViewerCLI.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class EventDetectorTest {
    
    private EventDetector detector;
    private WorkflowRunDTO testRun;
    private WorkflowJobDTO testJob;
    
    @BeforeEach
    void setUp() {
        detector = new EventDetector();
        testRun = new WorkflowRunDTO(
            1L,
            "Test Workflow",
            "main",
            "abc123",
            "queued",
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            "https://api.github.com/repos/owner/repo/actions/runs/1/jobs"
        );
        testJob = new WorkflowJobDTO(
            10L,
            "Test Job",
            "queued",
            null,
            null,
            null,
            List.of()
        );
    }
    
    @Test
    void testDetectNewWorkflowStarted() {
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(testRun, List.of());
        MonitorState emptyState = MonitorState.empty();
        
        List<Event> events = detector.detectEvents(runsWithJobs, emptyState);
        
        assertEquals(1, events.size());
        assertEquals(EventType.WORKFLOW_STARTED, events.get(0).type());
    }
    
    @Test
    void testDetectWorkflowCompleted() {
        WorkflowRunDTO completedRun = new WorkflowRunDTO(
            1L,
            "Test Workflow",
            "main",
            "abc123",
            "completed",
            "success",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "https://api.github.com/repos/owner/repo/actions/runs/1/jobs"
        );
        
        RunState previousState = new RunState(1L, "in_progress", null, Map.of());
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(completedRun, List.of());
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.WORKFLOW_COMPLETED, events.get(0).type());
    }
    
    @Test
    void testDetectNewJobStarted() {
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(testJob)
        );
        
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of());
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.JOB_STARTED, events.get(0).type());
    }
    
    @Test
    void testDetectJobCompleted() {
        WorkflowJobDTO completedJob = new WorkflowJobDTO(
            10L,
            "Test Job",
            "completed",
            "success",
            null,
            null,
            List.of()
        );
        
        JobState previousJobState = new JobState(10L, "in_progress", null, Map.of());
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(completedJob)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.JOB_COMPLETED, events.get(0).type());
    }
    
    @Test
    void testDetectNewStepStarted() {
        StepDto step = new StepDto("Build", "in_progress", null, 1, null, null);
        WorkflowJobDTO jobWithSteps = new WorkflowJobDTO(
            10L,
            "Test Job",
            "in_progress",
            null,
            null,
            null,
            List.of(step)
        );
        
        JobState previousJobState = new JobState(10L, "in_progress", null, Map.of());
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(jobWithSteps)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.STEP_STARTED, events.get(0).type());
    }
    
    @Test
    void testDetectStepCompleted() {
        StepDto completedStep = new StepDto("Build", "completed", "success", 1, null, null);
        WorkflowJobDTO jobWithSteps = new WorkflowJobDTO(
            10L,
            "Test Job",
            "in_progress",
            null,
            null,
            null,
            List.of(completedStep)
        );
        
        StepState previousStepState = new StepState("in_progress", null, "Build");
        JobState previousJobState = new JobState(10L, "in_progress", null, Map.of("Build", previousStepState));
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(jobWithSteps)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.STEP_COMPLETED, events.get(0).type());
    }
    
    @Test
    void testDetectStepFailed() {
        StepDto failedStep = new StepDto("Build", "completed", "failure", 1, null, null);
        WorkflowJobDTO jobWithSteps = new WorkflowJobDTO(
            10L,
            "Test Job",
            "in_progress",
            null,
            null,
            null,
            List.of(failedStep)
        );
        
        StepState previousStepState = new StepState("in_progress", null, "Build");
        JobState previousJobState = new JobState(10L, "in_progress", null, Map.of("Build", previousStepState));
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(jobWithSteps)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(1, events.size());
        assertEquals(EventType.STEP_FAILED, events.get(0).type());
    }
    
    @Test
    void testNoEventsWhenNothingChanges() {
        JobState previousJobState = new JobState(10L, "queued", null, Map.of());
        RunState previousRunState = new RunState(1L, "queued", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            testRun,
            List.of(testJob)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(0, events.size());
    }
    
    @Test
    void testDetectMultipleEvents() {
        StepDto step1 = new StepDto("Setup", "completed", "success", 1, null, null);
        StepDto step2 = new StepDto("Build", "in_progress", null, 2, null, null);
        
        WorkflowJobDTO completedJob = new WorkflowJobDTO(
            10L,
            "Test Job",
            "completed",
            "success",
            null,
            null,
            List.of(step1, step2)
        );
        
        WorkflowRunDTO completedRun = new WorkflowRunDTO(
            1L,
            "Test Workflow",
            "main",
            "abc123",
            "completed",
            "success",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "https://api.github.com/repos/owner/repo/actions/runs/1/jobs"
        );
        
        StepState previousStep1State = new StepState("in_progress", null, "Setup");
        JobState previousJobState = new JobState(10L, "in_progress", null, Map.of("Setup", previousStep1State));
        RunState previousRunState = new RunState(1L, "in_progress", null, Map.of(10L, previousJobState));
        MonitorState state = new MonitorState(
            LocalDateTime.now().atZone(ZoneId.systemDefault()),
            Map.of(1L, previousRunState)
        );
        
        Map<WorkflowRunDTO, List<WorkflowJobDTO>> runsWithJobs = Map.of(
            completedRun,
            List.of(completedJob)
        );
        List<Event> events = detector.detectEvents(runsWithJobs, state);
        
        assertEquals(4, events.size());
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.WORKFLOW_COMPLETED));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.JOB_COMPLETED));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.STEP_COMPLETED));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.STEP_STARTED));
    }
}