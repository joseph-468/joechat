package net.josephalba.joechat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;

public class Server extends Thread {
    public ArrayList<ServerInputThread> inputThreads = new ArrayList<>();
    public ArrayList<ServerOutputThread> outputThreads = new ArrayList<>();
    public ArrayList<Integer> userIds = new ArrayList<>();
    public ArrayList<String> usernames = new ArrayList<>();
    public Gui gui;

    Server(Gui gui) {
       this.gui = gui;
    }

    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(Main.PORT);
        }
        catch (Exception e) {
            e.printStackTrace();
            gui.showError("Failed to start server");
            return;
        }
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (Exception e) {
                e.printStackTrace();
                gui.showError("Failed to accept connection");
                return;
            }

            ServerInputThread inputThread = new ServerInputThread(gui, this, socket);
            ServerOutputThread outputThread = new ServerOutputThread(this, socket);
            inputThreads.add(inputThread);
            outputThreads.add(outputThread);
            inputThread.start();
            outputThread.start();
        }
    }
}

class ServerInputThread extends Thread {
    private final Socket socket;
    private final Server server;
    private final Gui gui;

    ServerInputThread(Gui gui, Server server, Socket socket) {
        this.gui = gui;
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        DataInputStream inputStream;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        int userId;
        String username;
        try {
            userId = inputStream.readInt();
            username = inputStream.readUTF();
        }
        catch (Exception e) {
            synchronized(server) {
                int i = server.inputThreads.indexOf(this);
                if (i != -1) {
                    server.inputThreads.remove(i);
                    server.outputThreads.remove(i);
                }
            }
            e.printStackTrace();
            return;
        }

        synchronized (server) {
            if (gui.secure && (server.usernames.contains(username) || server.userIds.contains(userId))) {
                int i = server.inputThreads.indexOf(this);
                if (i != -1) {
                    if (!socket.isClosed()) {
                        try {
                            socket.close();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    server.inputThreads.remove(i);
                    server.outputThreads.remove(i);
                }
                return;
            }

            server.usernames.add(username);
            server.userIds.add((Integer) userId);
        }

        String message;
        while (true) {
            try {
                message = inputStream.readUTF();
            }
            catch (Exception e) {
                synchronized(server) {
                    int i = server.inputThreads.indexOf(this);
                    if (i != -1) {
                        server.usernames.remove(username);
                        server.userIds.remove((Integer) userId);
                        server.inputThreads.remove(i);
                        server.outputThreads.remove(i);
                    }
                }
                e.printStackTrace();
                return;
            }
            for (ServerOutputThread outputThread : server.outputThreads) {
                synchronized (outputThread) {
                    outputThread.userId = userId;
                    outputThread.username = username;
                    outputThread.message = message;
                    outputThread.notify();
                }
            }
        }
    }
}

class ServerOutputThread extends Thread {
    private DataOutputStream outputStream;
    private final Server server;
    public int userId;
    public String username;
    public String message;

    ServerOutputThread(Server server, Socket socket) {
        this.server = server;

        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        synchronized (this) {
            while (true) {
                try {
                    wait();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                String timestamp = Instant.now().toString();

                try {
                    outputStream.writeInt(userId);
                    outputStream.writeUTF(timestamp);
                    outputStream.writeUTF(username);
                    outputStream.writeUTF(message);
                    outputStream.flush();
                }
                catch (Exception e) {
                    synchronized(server) {
                        int i = server.inputThreads.indexOf(this);
                        if (i != -1) {
                            server.usernames.remove(username);
                            server.userIds.remove((Integer) userId);
                            server.inputThreads.remove(i);
                            server.outputThreads.remove(i);
                        }
                    }
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}