package net.josephalba.joechat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Gui {
    public final boolean headless;
    public final boolean secure;
    private JFrame frame;
    private JTextPane chatPane;
    private int lineWrapThreshold;

    public Gui(String version, boolean headless, boolean secure) {
        this.headless = headless;
        this.secure = secure;
        if (headless) {
            Server server = new Server(this);
            server.start();
            return;
        }

        JPanel content = new JPanel();
        content.setPreferredSize(new Dimension(800, 600));

        frame = new JFrame("JoeChat " + version);
        // Default look and feel is acceptable if this fails
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        catch (Exception ignored) {}

        setDefaultFont("Monospaced", Font.PLAIN, 14);

        // Ensures clicking somewhere else removes focus from component
        frame.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.pack();
        frame.setVisible(true);
        startMainMenu();
    }

    public void startMainMenu() {
        if (headless) return;
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
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            updateFrame();
        });
    }

    public void startClientMainMenu() {
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            FocusListener removeTextOnFocus = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    final JTextField field = (JTextField) e.getSource();
                    final String defaultText = (String) field.getClientProperty("defaultText");

                    if (field.getText().equals(defaultText)) {
                        field.setText("");
                    }
                }

                public void focusLost(FocusEvent e) {
                    final JTextField field = (JTextField) e.getSource();
                    final String defaultText = (String) field.getClientProperty("defaultText");

                    if (field.getText().isEmpty()) {
                        field.setText(defaultText);
                    }
                }
            };

            resetFrame();

            JLabel addressLabel = new JLabel("Server IP address");
            addressLabel.setBounds(450, 150, 200, 50);
            addressLabel.setHorizontalAlignment(JLabel.CENTER);
            JTextField addressField = new JTextField("josephalba.net");
            addressField.putClientProperty("defaultText", "josephalba.net");
            addressField.setBounds(450, 200, 200, 50);
            addressField.addFocusListener(removeTextOnFocus);

            JLabel usernameLabel = new JLabel("Username");
            usernameLabel.setBounds(150, 150, 200, 50);
            usernameLabel.setHorizontalAlignment(JLabel.CENTER);
            JTextField usernameField = new JTextField("user");
            usernameField.putClientProperty("defaultText", "user");
            usernameField.setBounds(150, 200, 200, 50);
            usernameField.addFocusListener(removeTextOnFocus);

            JButton connectButton = new JButton("Connect");
            connectButton.setBounds(300, 300, 200, 50);
            connectButton.addActionListener(e -> {
                String username = usernameField.getText();
                if (username.equals("SERVER")) {
                    showError("Nice try");
                    return;
                }
                if (username.length() < 4 || username.length() > 12) {
                    showError("Username must be between 4 and 12 characters");
                    return;
                }
                Client client = new Client(this, username, addressField.getText());
                client.start();
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
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JButton exitButton = new JButton("Exit");
            exitButton.setBounds(5, 5, 85, 40);
            exitButton.addActionListener(e -> {
                client.close();
                startMainMenu();
            });
            frame.add(exitButton);

            JLabel addressLabel = new JLabel("You are connected to: ".concat(client.address));
            addressLabel.setFont(addressLabel.getFont().deriveFont(Font.PLAIN, 16));
            addressLabel.setBounds(100, 0, 500, 50);
            frame.add(addressLabel);

            JTextField messageBox = new JTextField();
            messageBox.setBounds(5, 555, 700, 40);
            messageBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            // By requesting focus typing always goes into the message box
            messageBox.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    SwingUtilities.invokeLater(messageBox::requestFocusInWindow);
                }
            });

            messageBox.addActionListener(e -> {
                sendMessage(client, messageBox);
            });

            frame.add(messageBox);
            messageBox.requestFocusInWindow();

            JButton chatSendButton = new JButton("Send");
            chatSendButton.setBounds(710, 555, 85, 40);
            chatSendButton.setFont(chatSendButton.getFont().deriveFont(Font.BOLD));
            chatSendButton.addActionListener(e -> {
                sendMessage(client, messageBox);
            });

            frame.add(chatSendButton);

            chatPane = new JTextPane();
            chatPane.setEditable(false);
            chatPane.setHighlighter(null);
            chatPane.setBorder(null);

            int chatWidth = 800;
            JScrollPane chatScrollPane = new JScrollPane(chatPane);
            chatScrollPane.setBounds(0, 50, chatWidth + 20, 500);
            chatScrollPane.setBorder(null);
            chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

            frame.getContentPane().add(chatScrollPane);

            lineWrapThreshold = calculateLineWrapThreshold(chatWidth);

            updateFrame();
        });
    }

    public void updateChatMessages(String message) {
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            String text = chatPane.getText();
            text = text.concat(message);
            chatPane.setText(text);
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
        });
    }

    public void showError(String error) {
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            resetFrame();

            JButton errorButton = new JButton("ERROR: " + error);
            errorButton.setBounds(200, 175,  400, 100);
            Font errorFont = new Font(errorButton.getFont().getFontName(), Font.BOLD, 12);
            errorButton.setFont(errorFont);
            errorButton.addActionListener(e -> {
                startMainMenu();
            });
            frame.add(errorButton);

            updateFrame();
        });
    }

    private void sendMessage(Client client, JTextField messageBox) {
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            String messageContent = messageBox.getText();
            if (messageContent.isEmpty() || messageContent.length() > 200) return;

            ArrayList<String> messageLines = new ArrayList<>();
            for (int i = 0; i < messageContent.length(); i += lineWrapThreshold) {
                messageLines.add(messageContent.substring(i, Math.min((i + lineWrapThreshold), messageContent.length())));
            }

            StringBuilder formattedMessage = new StringBuilder();
            for (int i = 0; i < messageLines.size(); i++) {
                if (i > 0) formattedMessage.append("                      ");
                formattedMessage.append(messageLines.get(i));
                formattedMessage.append("\n");
            }

            synchronized (client.outputThread) {
                client.outputThread.message = formattedMessage.toString();
                client.outputThread.notify();
            }
            messageBox.setText("");
        });
    }

    private void setDefaultFont(String name, int style, int size) {
        Font font = new Font(name, style, size);

        UIManager.put("Button.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("Label.font", font);
    }

    private int calculateLineWrapThreshold(int chatWidth) {
        FontMetrics fontMetrics = chatPane.getFontMetrics(chatPane.getFont());
        int charWidth = fontMetrics.charWidth(' ');
        int reservedSpace = 22;
        return chatWidth / charWidth - reservedSpace;
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
