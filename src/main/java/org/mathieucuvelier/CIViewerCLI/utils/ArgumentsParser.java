package org.mathieucuvelier.CIViewerCLI.utils;

public class ArgumentsParser {
    public static String[] parse(String[] rawArgs) {
        if (rawArgs.length < 3) {
            printUsage();
            System.exit(1);
        }
        return new String[] {rawArgs[0], rawArgs[1], rawArgs[2]};
    }

    public static void printUsage() {
        System.out.println("Usage: java -jar CIViewerCLI.jar <owner> <repo> <token>");
    }
}
