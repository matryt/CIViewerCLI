package org.mathieucuvelier.CIViewerCLI.utils;

public enum AnsiColors {
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    BLUE("\u001B[34m"),
    GRAY("\u001B[90m");

    private final String code;

    AnsiColors(String code) {
        this.code = code;
    }
    
    public String colorize(String text) {
        return code + text + RESET.code;
    }
}
