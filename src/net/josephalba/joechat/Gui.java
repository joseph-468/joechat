package net.josephalba.joechat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class Gui {
    public JTextArea textArea;
    private final JFrame frame;

    public Gui(String version) {
        frame = new JFrame("JoeChat " + version);
        // Default look and feel is acceptable if this fails
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception _) {}
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setVisible(true);
        startMainMenu();
    }

    public void startMainMenu() {
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JButton serverButton = new JButton("Host a server");
            serverButton.setBounds(150, 200, 200, 50);
            serverButton.addActionListener(e -> {
                Server server = new Server(this);
                server.start();
                startServerMainMenu(server);
            });
            frame.add(serverButton);

            JButton clientButton = new JButton("Connect to a server");
            clientButton.setBounds(450, 200, 200, 50);
            clientButton.addActionListener(e -> {
                startClientMainMenu();
            });
            frame.add(clientButton);

            updateFrame();
        });
    }

    public void startServerMainMenu(Server server) {
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JButton testButton = new JButton("Send message to all clients");
            testButton.setBounds(150, 150, 200, 50);
            testButton.addActionListener(e -> {
                for (ServerOutputThread outputThread : server.outputThreads) {
                    synchronized (outputThread) {
                        outputThread.message = "Test message";
                        outputThread.notify();
                    }
                }

            });
            frame.add(testButton);

            updateFrame();
        });
    }

    public void startClientMainMenu() {
        SwingUtilities.invokeLater(() -> {
            FocusListener removeTextOnFocus = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    JTextField field = (JTextField) e.getSource();
                    field.setText("");
                }

                public void focusLost(FocusEvent e) {}
            };

            resetFrame();

            JLabel addressLabel = new JLabel("Server IP address");
            addressLabel.setBounds(450, 150, 200, 50);
            addressLabel.setHorizontalAlignment(JLabel.CENTER);
            JTextField addressField = new JTextField("localhost");
            addressField.setBounds(450, 200, 200, 50);
            addressField.addFocusListener(removeTextOnFocus);

            JLabel usernameLabel = new JLabel("Username");
            usernameLabel.setBounds(150, 150, 200, 50);
            usernameLabel.setHorizontalAlignment(JLabel.CENTER);
            JTextField usernameField = new JTextField("user");
            usernameField.setBounds(150, 200, 200, 50);
            usernameField.addFocusListener(removeTextOnFocus);

            JButton connectButton = new JButton("Connect");
            connectButton.setBounds(300, 300, 200, 50);
            connectButton.addActionListener(e -> {
                String username = usernameField.getText();
                if (username.isEmpty() || username.length() > 16) return;
                Client client = new Client(this, username, addressField.getText());
                client.start();
                startClientChatMenu(client);
            });

            frame.add(addressLabel);
            frame.add(addressField);
            frame.add(usernameField);
            frame.add(usernameLabel);
            frame.add(connectButton);

            updateFrame();
        });
    }

    public void startClientChatMenu(Client client) {
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JTextField messageBox = new JTextField();
            messageBox.setBounds(150, 400, 400, 50);
            messageBox.addActionListener(e -> {
                synchronized (client.outputThread) {
                    client.outputThread.message = client.username + " #" + client.id + ": " + messageBox.getText();
                    client.outputThread.notify();
                }
            });
            frame.add(messageBox);

            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBounds(150, 50, 400, 300);

            frame.getContentPane().setLayout(null);
            frame.getContentPane().add(scrollPane);

            updateFrame();

        });
    }

    public void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JButton errorButton = new JButton("ERROR: " + error);
            errorButton.setBounds(200, 175,  400, 100);
            Font errorFont = new Font(errorButton.getFont().getFontName(), Font.BOLD, 14);
            errorButton.setFont(errorFont);
            errorButton.addActionListener(e -> {
                startMainMenu();
            });
            frame.add(errorButton);

            updateFrame();
        });
    }

    private void resetFrame() {
        frame.getContentPane().removeAll();
        frame.revalidate();
        frame.repaint();
    }

    private void updateFrame() {
        frame.revalidate();
        frame.repaint();
    }
}
