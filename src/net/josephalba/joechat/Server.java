package net.josephalba.joechat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    public ArrayList<ServerInputThread> inputThreads = new ArrayList<>();
    public ArrayList<ServerOutputThread> outputThreads = new ArrayList<>();

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(Main.PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ServerInputThread inputThread = new ServerInputThread(this, socket);
                ServerOutputThread outputThread = new ServerOutputThread( socket);
                inputThreads.add(inputThread);
                outputThreads.add(outputThread);
                inputThread.start();
                outputThread.start();
            }

            //serverSocket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ServerInputThread extends Thread {
    private final Socket socket;
    private final Server server;

    ServerInputThread(Server server, Socket socket) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            String message;
            while (true) {
                message = inputStream.readUTF();
                for (ServerOutputThread outputThread : server.outputThreads) {
                    synchronized (outputThread) {
                        outputThread.message = message;
                        outputThread.notify();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ServerOutputThread extends Thread {
    private DataOutputStream outputStream;
    public String message = "";

    ServerOutputThread(Socket socket) {
        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        synchronized (this) {
            while (true) {
                try {
                    wait();
                    try {
                        outputStream.writeUTF(message);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}