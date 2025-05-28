package net.josephalba.joechat;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Thread {
    public final String username;
    public final String address;
    public Socket socket;
    public ClientOutputThread outputThread;
    public ClientInputThread inputThread;
    public Gui gui;

    public Client(Gui gui, String username, String address) {
        this.gui = gui;
        this.username = username;
        this.address = address;
    }

    public void run() {
        try {
            socket = new Socket(InetAddress.getByName(address), Main.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputThread = new ClientInputThread(socket, username, address);
        inputThread.gui = gui;
        outputThread = new ClientOutputThread(socket);
        inputThread.start();
        outputThread.start();

        synchronized (this) {
            try {
                wait();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class ClientInputThread extends Thread {
    private final Socket socket;
    public String received;
    public Gui gui;

    public ClientInputThread(Socket socket, String username, String address) {
        this.socket = socket;
    }

    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            synchronized (gui) {
                while (true) {
                    received = inputStream.readUTF();
                    JTextArea textArea = gui.textArea;
                    String text = textArea.getText();
                    text = text.concat("\n");
                    text = text.concat(received);
                    textArea.setText(text);
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientOutputThread extends Thread {
    private final Socket socket;
    public String message = "";

    public ClientOutputThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        DataOutputStream outputStream = null;
        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (this) {
            while (true) {
                try {
                    wait();
                    outputStream.writeUTF(message);
                    message = "";
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
