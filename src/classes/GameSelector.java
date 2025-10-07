package classes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameSelector extends JFrame {

    private final GameClient client;
    private JPanel gamePanel;
    private JLabel statusLabel;
    private boolean matchInProgress = false;
    private TicTacToeGame board;

    public GameSelector(GameClient client) {
        this.client = client;

        setTitle("Game Selector");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Kies een spel:", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        add(label, BorderLayout.NORTH);

        gamePanel = new JPanel(new GridLayout(0, 1, 10, 10));
        add(new JScrollPane(gamePanel), BorderLayout.CENTER);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        add(statusLabel, BorderLayout.SOUTH);

        loadGames();

        // Attach the centralized match handler
        MatchHandler.handleIncomingChallenge(client);

        client.addServerListener(message -> {
            if (message.contains("SVR GAME MATCH")) {
                SwingUtilities.invokeLater(() -> {
                    if (board == null || !board.isDisplayable()) {
                        board = new TicTacToeGame(client);
                        board.setVisible(true);
                    }
                });
            }
        });


        setVisible(true);
    }

    private void loadGames() {
        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                JButton button = new JButton(gameName);
                button.setFont(new Font("Arial", Font.PLAIN, 14));
                button.addActionListener(e -> handleGameSelection(gameName));
                gamePanel.add(button);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Kon geen gamelist ophalen: " + e.getMessage(),
                    "Fout", JOptionPane.ERROR_MESSAGE);
        }

        gamePanel.revalidate();
        gamePanel.repaint();
    }

    private void handleGameSelection(String gameName) {
        if (!gameName.equalsIgnoreCase("tic-tac-toe")) {
            JOptionPane.showMessageDialog(this, gameName + " is not available yet.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (matchInProgress) {
            JOptionPane.showMessageDialog(this, "You are already in a match.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new GameModeSelector(client, gameName);
    }

    private String parseValue(String message, String key) {
        try {
            int idx = message.indexOf(key + ":");
            if (idx < 0) return null;
            int start = message.indexOf("\"", idx) + 1;
            int end = message.indexOf("\"", start);
            return message.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
