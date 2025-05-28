package net.josephalba.joechat;

public class Main {
    public static final int PORT = 8046;
    public static final String VERSION = "v0.1.0"; // Temporary solution

    public static void main(String[] args) {
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

        Gui gui = new Gui(VERSION);
        gui.start();
    }
}