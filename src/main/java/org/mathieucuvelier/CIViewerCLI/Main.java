package org.mathieucuvelier.CIViewerCLI;

import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.mathieucuvelier.CIViewerCLI.models.Config;
import org.mathieucuvelier.CIViewerCLI.service.GithubClient;
import org.mathieucuvelier.CIViewerCLI.service.WorkflowMonitor;
import org.mathieucuvelier.CIViewerCLI.utils.AnsiColors;
import org.mathieucuvelier.CIViewerCLI.utils.ConsoleLogger;

public class Main {

    public static void main(String[] args) {
        Config config;
        try {
            config = Config.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        GithubClient githubClient = new GithubClient(config.getOwner(), config.getRepo(), config.getToken());
        if (!githubClient.validateGithubToken()) {
            System.err.println(AnsiColors.RED.colorize("Invalid GitHub token provided. Please check your token and try again."));
            return;
        }

        Consumer<String> logger = new ConsoleLogger();
        WorkflowMonitor monitor;
        try {
            monitor = new WorkflowMonitor(githubClient, config.getOwner(), config.getRepo(), logger);
        } catch (SQLException | IOException e) {
            System.out.println("Problem while initializing monitoring !");
            System.out.println(e.getMessage());
            return;
        }
        monitor.startMonitoring();
    }
}
