package net.josephalba.joechat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, Main.PORT), 250); // Timeout in ms
        } catch (Exception e) {
            gui.showError("Could not connect to server");
            return;
        }

        inputThread = new ClientInputThread(gui, socket, username, address);
        outputThread = new ClientOutputThread(socket);
        inputThread.start();
        outputThread.start();

        gui.startClientChatMenu(this);

        synchronized (this) {
            try {
                wait();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        inputThread.running = false;
        try {
            socket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        outputThread.running = false;
        outputThread.interrupt();
    }
}

class ClientInputThread extends Thread {
    private final Socket socket;
    public Gui gui;
    public volatile boolean running;

    public ClientInputThread(Gui gui, Socket socket, String username, String address) {
        this.gui = gui;
        this.socket = socket;
    }

    public void run() {
        running = true;
        DataInputStream inputStream;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        synchronized (gui) {
            String timestamp;
            String message;
            while (true) {
                try {
                    timestamp = inputStream.readUTF();
                    message = inputStream.readUTF();
                }
                catch (Exception e) {
                    if (running) {
                        gui.showError("Lost connection to server");
                        e.printStackTrace();
                    }
                    return;
                }

                if (!gui.headless) {
                    ZonedDateTime zonedTimestamp = ZonedDateTime.ofInstant(Instant.parse(timestamp), ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    String formattedTimestamp = formatter.format(zonedTimestamp);

                    String formattedMessage = "[".concat(formattedTimestamp).concat("] ").concat(message);

                    gui.updateChatMessages(formattedMessage);
                }
            }
        }
    }
}

class ClientOutputThread extends Thread {
    private final Socket socket;
    public String message = "";
    public volatile boolean running;

    public ClientOutputThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        running = true;
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
                    if (running) {
                        e.printStackTrace();
                    }
                }

                try {
                    outputStream.writeUTF(message);
                    outputStream.flush();
                }
                catch (Exception e) {
                    if (running) {
                        e.printStackTrace();
                    }
                    return;
                }
                message = "";
            }
        }
    }
}
