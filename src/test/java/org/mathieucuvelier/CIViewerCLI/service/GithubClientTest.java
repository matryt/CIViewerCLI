package org.mathieucuvelier.CIViewerCLI.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mathieucuvelier.CIViewerCLI.mappers.ResponseBodyMapper;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubClientTest {

    private GithubClient githubClient;
    private HttpClient mockHttpClient;
    private ResponseBodyMapper mockMapper;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockMapper = mock(ResponseBodyMapper.class);
        githubClient = new GithubClient("owner", "repo", "token") {
            @Override
            public HttpClient getHttpClient() {
                return mockHttpClient;
            }

            @Override
            public ResponseBodyMapper getResponseBodyMapper() {
                return mockMapper;
            }
        };
    }

    @Test
    void testGetWorkflowRuns_Success() throws Exception {
        ZonedDateTime dateTime = ZonedDateTime.now().minusDays(1);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"workflow_runs\": []}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        List<WorkflowRunDTO> result = githubClient.getWorkflowRuns(dateTime);

        assertNotNull(result);
        verify(mockMapper).deserializeWorkflowRuns(anyString());
    }

    @Test
    void testGetWorkflowRuns_Failure() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        List<WorkflowRunDTO> result = githubClient.getWorkflowRuns(ZonedDateTime.now());

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetWorkflowRuns_Exception() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new RuntimeException("Test exception"));

        List<WorkflowRunDTO> result = githubClient.getWorkflowRuns(ZonedDateTime.now());

        assertTrue(result.isEmpty());
    }
}