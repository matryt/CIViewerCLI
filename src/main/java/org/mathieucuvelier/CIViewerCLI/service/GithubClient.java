package org.mathieucuvelier.CIViewerCLI.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.ZonedDateTime;
import java.util.List;

import org.mathieucuvelier.CIViewerCLI.mappers.ResponseBodyMapper;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import java.net.http.HttpResponse;

public class GithubClient {
    private static final String baseUrl = "https://api.github.com/repos/";
    private final String urlForRepo;
    private final String token;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ResponseBodyMapper responseBodyMapper = new ResponseBodyMapper();

    public GithubClient(String owner, String repo, String token) {
        this.urlForRepo = baseUrl + owner + "/" + repo;
        this.token = token;
    }

    public List<WorkflowRunDTO> getWorkflowRuns(ZonedDateTime datetime) {
        String url = urlForRepo + "/actions/runs?per_page=100";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return responseBodyMapper.deserializeWorkflowRuns(responseBody).stream()
                        .filter(run -> run.isAfter(datetime))
                        .toList();
            } else {
                System.err.println("Failed to fetch workflow runs. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public List<WorkflowJobDTO> getWorkflowJobs(String jobsUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jobsUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return responseBodyMapper.deserializeWorkflowJobs(responseBody);
            } else {
                System.err.println("Failed to fetch workflow jobs. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

}
