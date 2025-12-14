package org.mathieucuvelier.CIViewerCLI;

import java.io.IOException;
import java.sql.SQLException;

import org.mathieucuvelier.CIViewerCLI.persistence.DatabaseManager;
import org.mathieucuvelier.CIViewerCLI.service.GithubClient;
import org.mathieucuvelier.CIViewerCLI.service.WorkflowMonitor;
import org.mathieucuvelier.CIViewerCLI.utils.ArgumentsParser;

public class Main {

    public static void main(String[] args) {
        String[] parsedArgs = ArgumentsParser.parse(args);
        String owner = parsedArgs[0];
        String repo = parsedArgs[1];
        String token = parsedArgs[2];

        GithubClient githubClient = new GithubClient(owner, repo, token);
        WorkflowMonitor monitor;
        try {
            monitor = new WorkflowMonitor(githubClient, owner, repo);
        } catch (SQLException | IOException e) {
            System.out.println("Problem while initializing monitoring !");
            System.out.println(e.getMessage());
            return;
        }
        monitor.startMonitoring();
    }
}
