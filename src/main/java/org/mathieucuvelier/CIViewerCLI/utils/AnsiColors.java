package org.mathieucuvelier.CIViewerCLI.utils;

public enum AnsiColors {
    RESET("\u001B[0m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    GRAY("\u001B[90m"),
    WHITE("\u001B[37m");

    private final String code;

    AnsiColors(String code) {
        this.code = code;
    }
    
    public String colorize(String text) {
        return code + text + RESET.code;
    }
}
