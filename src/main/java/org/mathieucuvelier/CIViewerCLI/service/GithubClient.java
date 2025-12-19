package org.mathieucuvelier.CIViewerCLI.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.Getter;
import org.mathieucuvelier.CIViewerCLI.mappers.ResponseBodyMapper;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowJobDTO;
import org.mathieucuvelier.CIViewerCLI.models.WorkflowRunDTO;
import java.net.http.HttpResponse;

public class GithubClient {
    private static final String baseUrl = "https://api.github.com/repos/";
    private final String urlForRepo;
    private final String token;
    @Getter
    private final HttpClient httpClient = HttpClient.newHttpClient();
    @Getter
    private final ResponseBodyMapper responseBodyMapper = new ResponseBodyMapper();

    public GithubClient(String owner, String repo, String token) {
        this.urlForRepo = baseUrl + owner + "/" + repo;
        this.token = token;
    }

    public List<WorkflowRunDTO> getWorkflowRuns(ZonedDateTime datetime) {
        String url = urlForRepo + "/actions/runs?per_page=100";
        try {
            return executeHttpRequestWithHandling(() -> {
                HttpRequest request = createRequestBuilder(url)
                        .GET()
                        .build();
                var response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    try {
                        return getResponseBodyMapper().deserializeWorkflowRuns(responseBody).stream()
                                .filter(run -> run.isAfter(datetime))
                                .toList();
                    } catch (IllegalArgumentException e) {
                        System.err.println("Failed to parse workflow runs. Here is the response of the Github API : " + responseBody);
                    }
                } else {
                    throw new HttpResponseException(response.statusCode(), "Failed to fetch workflow runs.");
                }
                return List.of();
            }, 3, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public List<WorkflowJobDTO> getWorkflowJobs(String jobsUrl) {
        try {
            return executeHttpRequestWithHandling(() -> {
                HttpRequest request = createRequestBuilder(jobsUrl)
                        .GET()
                        .build();
                var response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    try {
                        return getResponseBodyMapper().deserializeWorkflowJobs(responseBody);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Failed to parse workflow jobs. Here is the response of Github API : " + responseBody);
                    }
                } else {
                    throw new HttpResponseException(response.statusCode(), "Failed to fetch workflow jobs.");
                }
                return List.of();
            }, 3, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public boolean validateGithubToken() {
        String url = "https://api.github.com/user";
        try {
            return executeHttpRequestWithHandling(() -> {
                HttpRequest request = createRequestBuilder(url)
                        .GET()
                        .build();
                var response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return true;
                } else {
                    throw new HttpResponseException(response.statusCode(), "Invalid GitHub token.");
                }
            }, 3, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private <T> T executeHttpRequestWithHandling(Callable<T> action, int maxRetries, long initialDelayMillis) throws Exception {
        int attempt = 0;
        long delay = initialDelayMillis;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return action.call();
            } catch (HttpResponseException e) {
                int statusCode = e.getStatusCode();
                switch (statusCode) {
                    case 401:
                        System.err.println("Invalid GitHub token. Please check your credentials.");
                        System.exit(1);
                        break;
                    case 403:
                        System.err.println("Access forbidden. You might not have the necessary permissions.");
                        System.exit(1);
                        break;
                    case 429:
                        System.err.println("Rate limit exceeded. Waiting before retrying...");
                        Thread.sleep(delay);
                        break;
                    case 404:
                        System.err.println("Repository not found. Please check the owner and repo name.");
                        System.exit(1);
                        break;
                    default:
                        if (statusCode >= 500 && statusCode < 600) {
                            System.err.println("Server error (" + statusCode + "). Retrying...");
                            attempt++;
                        } else {
                            throw e;
                        }
                }
            } catch (Exception e) {
                lastException = e;
                attempt++;
                System.err.println("Retry attempt " + attempt + " failed: " + e.getMessage());
                if (attempt >= maxRetries) {
                    break;
                }
            }
            Thread.sleep(delay);
            delay *= 2; // Exponential backoff
        }

        System.err.println("All retry attempts failed.");
        throw lastException; // Preserve the original exception
    }

    private HttpRequest.Builder createRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
    }
}

@Getter
class HttpResponseException extends IOException {
    private final int statusCode;

    public HttpResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
