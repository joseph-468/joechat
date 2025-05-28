package net.josephalba.joechat;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Thread {
    public final String username;
    public final String address;
    public Socket socket;
    public ClientOutputThread outputThread;
    public ClientInputThread inputThread;
    public Gui gui;
    public int id;

    public Client(Gui gui, String username, String address) {
        this.gui = gui;
        this.username = username;
        this.address = address;
    }

    public void run() {
        String temp = System.getProperty("os.name") +
                      System.getProperty("os.version") +
                      System.getProperty("os.arch") +
                      System.getProperty("user.name") +
                      System.getProperty("java.version");
        id = Math.abs(temp.hashCode());

        try {
            socket = new Socket(InetAddress.getByName(address), Main.PORT);
        } catch (Exception e) {
            gui.showError("Could not connect to server");
            return;
        }

        inputThread = new ClientInputThread(gui, socket, username, address);
        outputThread = new ClientOutputThread(gui, socket);
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

    public ClientInputThread(Gui gui, Socket socket, String username, String address) {
        this.gui = gui;
        this.socket = socket;
    }

    public void run() {
        DataInputStream inputStream;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        synchronized (gui) {
            while (true) {
                try {
                    received = inputStream.readUTF();
                }
                catch (Exception e) {
                    gui.showError("Lost connection to server");
                    e.printStackTrace();
                    return;
                }
                JTextArea textArea = gui.textArea;
                String text = textArea.getText();
                text = text.concat("\n");
                text = text.concat(received);
                textArea.setText(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }
    }
}

class ClientOutputThread extends Thread {
    private final Socket socket;
    public String message = "";
    private final Gui gui;

    public ClientOutputThread(Gui gui, Socket socket) {
        this.gui = gui;
        this.socket = socket;
    }

    public void run() {
        DataOutputStream outputStream;
        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        synchronized (this) {
            while (true) {
                try {
                    wait();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    outputStream.writeUTF(message);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                message = "";
            }
        }
    }
}
