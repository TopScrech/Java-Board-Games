package classes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class LoginGUI extends JFrame {

    private JTextField nameField;
    private JButton loginButton;
    private JLabel statusLabel;
    private DefaultListModel<String> onlineListModel;
    private JList<String> onlineList;

    private GameClient client;

    public LoginGUI() {
        setTitle("TicTacToe Login");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        nameField = new JTextField();
        loginButton = new JButton("Login");
        statusLabel = new JLabel("Status: Niet ingelogd");
        topPanel.add(new JLabel("Voer je spelersnaam in:"));
        topPanel.add(nameField);
        topPanel.add(loginButton);
        add(topPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.SOUTH);

        onlineListModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineListModel);
        add(new JScrollPane(onlineList), BorderLayout.CENTER);

        loginButton.addActionListener(e -> login());

        setVisible(true);
    }

    private void login() {
        String playerName = nameField.getText().trim().toLowerCase();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Voer een geldige naam in.");
            return;
        }

        try {
            client = new GameClient("127.0.0.1", 7789, playerName);
            new Thread(client::startListening).start();
            client.login();
            statusLabel.setText("Status: Ingelogd als " + playerName);

            MatchHandler.attach(client);

            SwingUtilities.invokeLater(() -> {
                GameSelector selector = new GameSelector(client);
                MatchHandler.setParentFrame(client, selector);
            });

        } catch (IOException ex) {
            statusLabel.setText("Status: Login mislukt");
            JOptionPane.showMessageDialog(this, "Kan niet verbinden of login mislukt: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginGUI::new);
    }
}
