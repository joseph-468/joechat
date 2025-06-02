package net.josephalba.joechat;

import javax.swing.*;

public class Main {
    public static final int PORT = 8046;
    public static final String VERSION = "v0.10.0";

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + thread.getName());
            throwable.printStackTrace();
            System.exit(1);
        });

        boolean headless = false;
        boolean secure = false;
        for (String arg : args) {
            switch (arg) {
                case "--help" -> {
                    System.out.println("I don't know how to help");
                    return;
                }
                case "--version" -> {
                    System.out.println(VERSION);
                    return;
                }
                case "--headless" -> {
                    headless = true;
                }
                case "--secure" -> {
                    secure = true;
                }
            }
        }

        boolean finalHeadless = headless;
        boolean finalSecure = secure;
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui(VERSION, finalHeadless, finalSecure);
        });
    }
}