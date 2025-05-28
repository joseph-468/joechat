package net.josephalba.joechat;

import javax.swing.*;

public class Main {
    public static final int PORT = 8046;
    public static final String VERSION = "v0.2.1";

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + thread.getName());
            throwable.printStackTrace();
            System.exit(1);
        });

        for (String arg : args) {
            switch (arg) {
                case "--help" -> {
                    System.out.println("I don't know how to help");
                    return;
                }
                case "--headless" -> {
                    return;
                }
                case "--version" -> {
                    System.out.println(VERSION);
                    return;
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui(VERSION);
        });
    }
}