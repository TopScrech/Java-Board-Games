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
    private boolean loggedIn = false;

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
            loggedIn = true;
            statusLabel.setText("Status: Ingelogd als " + playerName);

            Timer timer = new Timer(2000, e -> updateOnlineList());
            timer.start();

            SwingUtilities.invokeLater(() -> new GameSelector(client));

        } catch (IOException ex) {
            loggedIn = false;
            statusLabel.setText("Status: Login mislukt");
            JOptionPane.showMessageDialog(this, "Kan niet verbinden of login mislukt: " + ex.getMessage());
        }
    }

    private void updateOnlineList() {
        if (!loggedIn) return;
        try {
            List<String> players = client.getPlayerList();
            onlineListModel.clear();
            for (String p : players) onlineListModel.addElement(p);
        } catch (IOException e) {
            statusLabel.setText("Status: Kan online lijst niet ophalen");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginGUI::new);
    }
}
