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
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Kies een spel:", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        add(label, BorderLayout.NORTH);

        gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(0, 1, 10, 10));
        JScrollPane scrollPane = new JScrollPane(gamePanel);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        add(statusLabel, BorderLayout.SOUTH);

        loadGames();

        setVisible(true);
    }

    private void loadGames() {
        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                JButton button = new JButton(gameName);
                button.setFont(new Font("Arial", Font.PLAIN, 14));
                button.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Click handler
                button.addActionListener(e -> handleGameSelection(gameName));

                gamePanel.add(button);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Kon geen gamelist ophalen: " + e.getMessage(),
                    "Fout", JOptionPane.ERROR_MESSAGE);
        }

        gamePanel.revalidate();
        gamePanel.repaint();
    }

    private void handleGameSelection(String gameName) {
        if (!gameName.equalsIgnoreCase("tic-tac-toe")) {
            JOptionPane.showMessageDialog(this,
                    gameName + " is nog niet beschikbaar.",
                    "Game niet beschikbaar",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (matchInProgress) {
            JOptionPane.showMessageDialog(this,
                    "Je zit al in een match.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Non-blocking mode selection frame
        JFrame modeFrame = new JFrame("Kies modus");
        modeFrame.setSize(300, 100);
        modeFrame.setLayout(new FlowLayout());
        modeFrame.setLocationRelativeTo(this);
        modeFrame.setAlwaysOnTop(true);
        modeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton playerBtn = new JButton("Spelen tegen speler");
        JButton aiBtn = new JButton("Spelen tegen AI");

        modeFrame.add(playerBtn);
        modeFrame.add(aiBtn);
        modeFrame.setVisible(true);

        playerBtn.addActionListener(e -> {
            modeFrame.dispose();
            startPvP(gameName);
        });

        aiBtn.addActionListener(e -> {
            modeFrame.dispose();
            startPvAI(gameName);
        });
    }

    private void startPvAI(String gameName) {
        board = new TicTacToeGame(null);
        board.setAIMode(true);
        board.setVisible(true);
        matchInProgress = true;
    }

    private void startPvP(String gameName) {
        statusLabel.setText("Waiting for other player...");
        matchInProgress = true;

        // Set server listener for automatic matchmaking
        client.setServerListener(message -> {
            try {
                if (message.startsWith("SVR PLAYERLIST")) {
                    List<String> players = parsePlayerList(message);
                    for (String p : players) {
                        if (!p.equalsIgnoreCase(client.getPlayerName())) {
                            client.challenge(p, gameName);
                            System.out.println("DEBUG: Automatically challenged " + p);
                            break;
                        }
                    }
                } else if (message.contains("SVR GAME CHALLENGE")) {
                    int challengeNumber = Integer.parseInt(parseValue(message, "CHALLENGENUMBER"));
                    client.acceptChallenge(challengeNumber);
                    System.out.println("DEBUG: Automatically accepted challenge #" + challengeNumber);
                } else if (message.contains("SVR GAME MATCH")) {
                    SwingUtilities.invokeLater(() -> {
                        String playerToMove = parseValue(message, "PLAYERTOMOVE");
                        String opponent = parseValue(message, "OPPONENT");

                        board = new TicTacToeGame(client);
                        board.setVisible(true);

                        if (playerToMove != null && playerToMove.equalsIgnoreCase(client.getPlayerName())) {
                            board.enablePlayerTurn();
                        }

                        statusLabel.setText("Match found! Opponent: " + opponent);
                        System.out.println("DEBUG: Match started with " + opponent + ", first to move: " + playerToMove);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            client.subscribe(gameName);
            System.out.println("DEBUG: Subscribed to " + gameName + " and waiting for match");

            List<String> players = client.getPlayerList();
            for (String p : players) {
                if (!p.equalsIgnoreCase(client.getPlayerName())) {
                    client.challenge(p, gameName);
                    System.out.println("DEBUG: Challenged " + p + " after getting player list");
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Kan niet inschrijven: " + ex.getMessage());
        }
    }

    private List<String> parsePlayerList(String message) {
        int start = message.indexOf('[');
        int end = message.indexOf(']');
        List<String> players = new java.util.ArrayList<>();
        if (start >= 0 && end > start) {
            String list = message.substring(start + 1, end);
            String[] names = list.replaceAll("\"", "").split(",");
            for (String name : names) if (!name.trim().isEmpty()) players.add(name.trim());
        }
        return players;
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
