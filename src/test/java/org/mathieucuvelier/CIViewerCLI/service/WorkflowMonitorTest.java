package org.mathieucuvelier.CIViewerCLI.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mathieucuvelier.CIViewerCLI.models.Event;
import org.mathieucuvelier.CIViewerCLI.persistence.MonitorState;
import org.mathieucuvelier.CIViewerCLI.persistence.StateManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowMonitorTest {

    private WorkflowMonitor workflowMonitor;
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        GithubClient mockGithubClient = mock(GithubClient.class);
        when(mockGithubClient.getWorkflowRuns(any()))
                .thenReturn(List.of());
        mockStateManager = mock(StateManager.class);
        when(mockStateManager.loadState(any(), any())).thenReturn(MonitorState.empty());
        workflowMonitor = new WorkflowMonitor(mockGithubClient, "owner", "repo", (msg) -> {System.out.println(msg);}) {
            @Override
            public StateManager getStateManager() {
                return mockStateManager;
            }
        };
    }

    @Test
    void testInitialization() {
        assertNotNull(workflowMonitor);
    }

    @Test
    void startMonitoring_doesNotThrow_andCanBeStopped() throws Exception {
        Thread t = new Thread(workflowMonitor::startMonitoring);
        t.start();
        // laisse tourner un peu
        Thread.sleep(200);
        workflowMonitor.stop();
        t.interrupt(); // au cas où il est dans sleep()
        t.join(1_000);
        assertFalse(t.isAlive(), "Le monitoring doit s'arrêter proprement");
    }
}