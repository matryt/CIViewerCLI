package org.mathieucuvelier.CIViewerCLI.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

public class StateManagerTest {
    
    private DatabaseManager dbManager;
    private StateManager stateManager;
    
    @BeforeEach
    void setUp() throws SQLException, IOException {
        dbManager = new DatabaseManager(":memory:");
        stateManager = new StateManager(dbManager);
    }
    
    @AfterEach
    void tearDown() {
        stateManager.close();
    }
    
    @Test
    void testLoadStateWhenEmpty() {
        MonitorState state = stateManager.loadState("owner", "repo");
        
        assertEquals(LocalDateTime.MIN.atZone(ZoneId.systemDefault()), state.lastCheckTimestamp());
        assertTrue(state.knownRuns().isEmpty());
    }
    
    @Test
    void testSaveAndLoadTimestamp() {
        ZonedDateTime now = ZonedDateTime.now();
        MonitorState state = new MonitorState(now, Map.of());
        
        stateManager.saveState("owner", "repo", state);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        assertEquals(now.toInstant(), loaded.lastCheckTimestamp().toInstant());
    }
    
    @Test
    void testSaveAndLoadSimpleRun() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        RunState run = new RunState(123L, "completed", "success", Map.of());
        MonitorState state = new MonitorState(timestamp, Map.of(123L, run));
        
        stateManager.saveState("owner", "repo", state);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        assertEquals(1, loaded.knownRuns().size());
        RunState loadedRun = loaded.knownRuns().get(123L);
        assertNotNull(loadedRun);
        assertEquals("completed", loadedRun.status());
        assertEquals("success", loadedRun.conclusion());
        assertTrue(loadedRun.knownJobs().isEmpty());
    }
    
    @Test
    void testSaveAndLoadRunWithJobs() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        
        JobState job1 = new JobState(10L, "completed", "success", Map.of());
        JobState job2 = new JobState(20L, "in_progress", null, Map.of());
        Map<Long, JobState> jobs = Map.of(10L, job1, 20L, job2);
        
        RunState run = new RunState(123L, "in_progress", null, jobs);
        MonitorState state = new MonitorState(timestamp, Map.of(123L, run));
        
        stateManager.saveState("owner", "repo", state);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        assertEquals(1, loaded.knownRuns().size());
        RunState loadedRun = loaded.knownRuns().get(123L);
        assertEquals(2, loadedRun.knownJobs().size());
        
        JobState loadedJob1 = loadedRun.knownJobs().get(10L);
        assertEquals("completed", loadedJob1.status());
        assertEquals("success", loadedJob1.conclusion());
        
        JobState loadedJob2 = loadedRun.knownJobs().get(20L);
        assertEquals("in_progress", loadedJob2.status());
        assertNull(loadedJob2.conclusion());
    }
    
    @Test
    void testSaveAndLoadCompleteHierarchy() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        
        StepState step1 = new StepState("completed", "success", "Build");
        StepState step2 = new StepState("in_progress", "", "Test");
        Map<String, StepState> steps = Map.of("Build", step1, "Test", step2);
        
        JobState job = new JobState(10L, "in_progress", "", steps);
        RunState run = new RunState(123L, "in_progress", "", Map.of(10L, job));
        MonitorState state = new MonitorState(timestamp, Map.of(123L, run));
        
        stateManager.saveState("owner", "repo", state);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        RunState loadedRun = loaded.knownRuns().get(123L);
        JobState loadedJob = loadedRun.knownJobs().get(10L);
        assertEquals(2, loadedJob.stepStates().size());
        
        StepState loadedStep1 = loadedJob.stepStates().get("Build");
        assertNotNull(loadedStep1);
        assertEquals("completed", loadedStep1.status());
        assertEquals("success", loadedStep1.conclusion());
        assertEquals("Build", loadedStep1.stepName());
        
        StepState loadedStep2 = loadedJob.stepStates().get("Test");
        assertNotNull(loadedStep2);
        assertEquals("in_progress", loadedStep2.status());
        assertTrue(loadedStep2.conclusion().isEmpty());
        assertEquals("Test", loadedStep2.stepName());
    }
    
    @Test
    void testMultipleRepositories() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        RunState run1 = new RunState(100L, "completed", "success", Map.of());
        RunState run2 = new RunState(200L, "in_progress", null, Map.of());
        
        MonitorState state1 = new MonitorState(timestamp, Map.of(100L, run1));
        MonitorState state2 = new MonitorState(timestamp, Map.of(200L, run2));
        
        stateManager.saveState("owner1", "repo1", state1);
        stateManager.saveState("owner2", "repo2", state2);
        
        MonitorState loaded1 = stateManager.loadState("owner1", "repo1");
        MonitorState loaded2 = stateManager.loadState("owner2", "repo2");
        
        assertEquals(1, loaded1.knownRuns().size());
        assertEquals(100L, loaded1.knownRuns().get(100L).runId());
        
        assertEquals(1, loaded2.knownRuns().size());
        assertEquals(200L, loaded2.knownRuns().get(200L).runId());
    }
    
    @Test
    void testUpdateExistingState() {
        ZonedDateTime timestamp1 = ZonedDateTime.now().minusMinutes(5);
        RunState run1 = new RunState(123L, "in_progress", null, Map.of());
        MonitorState state1 = new MonitorState(timestamp1, Map.of(123L, run1));
        
        stateManager.saveState("owner", "repo", state1);
        
        ZonedDateTime timestamp2 = ZonedDateTime.now();
        RunState run2 = new RunState(123L, "completed", "success", Map.of());
        MonitorState state2 = new MonitorState(timestamp2, Map.of(123L, run2));
        
        stateManager.saveState("owner", "repo", state2);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        assertEquals(timestamp2.toInstant(), loaded.lastCheckTimestamp().toInstant());
        RunState loadedRun = loaded.knownRuns().get(123L);
        assertEquals("completed", loadedRun.status());
        assertEquals("success", loadedRun.conclusion());
    }
    
    @Test
    void testSaveMultipleRuns() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        
        RunState run1 = new RunState(100L, "completed", "success", Map.of());
        RunState run2 = new RunState(200L, "completed", "failure", Map.of());
        RunState run3 = new RunState(300L, "in_progress", null, Map.of());
        
        Map<Long, RunState> runs = Map.of(100L, run1, 200L, run2, 300L, run3);
        MonitorState state = new MonitorState(timestamp, runs);
        
        stateManager.saveState("owner", "repo", state);
        MonitorState loaded = stateManager.loadState("owner", "repo");
        
        assertEquals(3, loaded.knownRuns().size());
        assertEquals("success", loaded.knownRuns().get(100L).conclusion());
        assertEquals("failure", loaded.knownRuns().get(200L).conclusion());
        assertEquals("in_progress", loaded.knownRuns().get(300L).status());
    }
}
