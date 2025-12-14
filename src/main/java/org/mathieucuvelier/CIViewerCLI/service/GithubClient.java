package org.mathieucuvelier.CIViewerCLI.service;

public class GithubClient {
    private static final String baseUrl = "https://api.github.com/repos/";
    private final String urlForRepo;

    public GithubClient(String owner, String repo) {
        this.urlForRepo = baseUrl + owner + "/" + repo;
    }

}
