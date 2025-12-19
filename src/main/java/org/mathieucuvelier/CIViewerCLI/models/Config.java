package org.mathieucuvelier.CIViewerCLI.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Configuration for the CI Viewer CLI tool.
 */
@RequiredArgsConstructor
@Getter
public class Config {
    private final String owner;
    private final String repo;
    private final String token;

    public static Config fromArgs(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Invalid arguments. Usage: java -jar tool.jar <owner> <repo> <token>");
        }
        return new Config(args[0], args[1], args[2]);
    }
}