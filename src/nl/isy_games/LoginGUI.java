package nl.isy_games;

import javax.swing.*;
import nl.isy_games.GameSelector;
import java.awt.*;
import java.io.IOException;

public class LoginGUI extends JFrame {

    private final JTextField nameField;
    private final JButton loginButton;
    private final JLabel statusLabel;
    private GameClient client;

    public LoginGUI() {
        setTitle("TicTacToe Login");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topPanel.add(new JLabel("Voer je spelersnaam in:"));
        nameField = new JTextField();
        topPanel.add(nameField);

        loginButton = new JButton("Login");
        topPanel.add(loginButton);

        statusLabel = new JLabel("Status: Niet ingelogd", SwingConstants.CENTER);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        setVisible(true);

        loginButton.addActionListener(e -> login());
    }

    private void login() {
        String playerName = nameField.getText().trim().toLowerCase();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Voer een geldige naam in.");
            return;
        }

        try {
            client = new GameClient("5.83.140.43", 7789, playerName);
            new Thread(client::startListening).start();
            client.login();
            statusLabel.setText("Status: Ingelogd als " + playerName);

            MatchHandler.attach(client);

            SwingUtilities.invokeLater(() -> {
                GameSelector selector = new GameSelector(client);
            });

            dispose();

        } catch (IOException ex) {
            statusLabel.setText("Status: Login mislukt");
            JOptionPane.showMessageDialog(this, "Kan niet verbinden of login mislukt: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginGUI::new);
    }
}
