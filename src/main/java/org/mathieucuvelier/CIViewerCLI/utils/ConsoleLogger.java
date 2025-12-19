package org.mathieucuvelier.CIViewerCLI.utils;

import java.util.function.Consumer;

public class ConsoleLogger implements Consumer<String> {

    @Override
    public void accept(String message) {
        System.out.println(message);
    }
}