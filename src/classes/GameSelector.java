package classes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Random;

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

        new GameModeSelector(client, gameName, this);
    }

    public void startPvAI(String gameName) {
        board = new TicTacToeGame(null);
        board.setAIMode(true);
        board.setVisible(true);
        matchInProgress = true;
        System.out.println("DEBUG: Started AI game for " + gameName);
    }

    public void startPvPRandom(String gameName) {
        if (matchInProgress) return;
        matchInProgress = true;

        System.out.println("DEBUG: Starting Random PvP matchmaking for " + gameName);
        statusLabel.setText("Searching for random opponent...");

        try {
            System.out.println("DEBUG: Subscribing for Random PvP: " + gameName);
            client.subscribe(gameName);
            System.out.println("DEBUG: Subscribed to " + gameName);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Subscribe failed: " + ex.getMessage());
            matchInProgress = false;
            return;
        }

        new Thread(() -> {
            try {
                client.listen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ServerListenerThread").start();

        client.setServerListener(message -> {
            System.out.println("DEBUG: Server message received: " + message);

            SwingUtilities.invokeLater(() -> {
                try {
                    if (message.contains("SVR GAME CHALLENGE")) {
                        int challengeNumber = Integer.parseInt(parseValue(message, "CHALLENGENUMBER"));
                        String challenger = parseValue(message, "CHALLENGER");
                        String gametype = parseValue(message, "GAMETYPE");

                        System.out.println("DEBUG: Received challenge #" + challengeNumber + " from " + challenger);

                        try {
                            client.acceptChallenge(challengeNumber);
                            System.out.println("DEBUG: Accepting challenge #" + challengeNumber + " from " + challenger);
                            matchInProgress = false;
                        } catch (Exception acceptEx) {
                            System.out.println("ERROR: Failed to accept challenge #" + challengeNumber + ": " + acceptEx.getMessage());
                        }
                    }

                    else if (message.contains("SVR GAME MATCH")) {
                        String playerToMove = parseValue(message, "PLAYERTOMOVE");
                        String opponent = parseValue(message, "OPPONENT");

                        System.out.println("DEBUG: MATCH CREATED! Opponent: " + opponent + ", first to move: " + playerToMove);
                        matchInProgress = false;

                        board = new TicTacToeGame(client);
                        board.setVisible(true);

                        if (playerToMove.equalsIgnoreCase(client.getPlayerName())) {
                            board.enablePlayerTurn();
                            System.out.println("DEBUG: It's my turn (" + client.getPlayerName() + ")");
                        } else {
                            System.out.println("DEBUG: Waiting for opponent to move...");
                        }

                        statusLabel.setText("Match found! Opponent: " + opponent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        new Thread(() -> {
            try {
                Thread.sleep(1500);

                List<String> players = client.getPlayerList();
                players.removeIf(p -> p.equalsIgnoreCase(client.getPlayerName()));

                if (players.isEmpty()) {
                    System.out.println("DEBUG: No players online yet, waiting...");
                    return;
                }

                for (String other : players) {
                    if (client.getPlayerName().compareToIgnoreCase(other) > 0) {
                        System.out.println("DEBUG: Skipping challenge to avoid double challenge conflict.");
                        continue;
                    }

                    System.out.println("DEBUG: Found possible opponent: " + other);
                    client.challenge(other, gameName);
                    System.out.println("DEBUG: Challenge sent to " + other + " for " + gameName);
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "MatchmakerThread").start();
    }

    public void startPvPFindPlayer(String gameName, String opponentName) {
        try {
            List<String> players = client.getPlayerList();
            if (!players.contains(opponentName)) {
                JOptionPane.showMessageDialog(this,
                        "No player found with that name.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            client.challenge(opponentName, gameName);
            statusLabel.setText("Challenge sent to " + opponentName + ", waiting for acceptance...");
            System.out.println("DEBUG: Challenge sent to " + opponentName);

            client.setServerListener(message -> {
                System.out.println("DEBUG: Server message received: " + message);
                if (message.contains("SVR GAME MATCH")) {
                    SwingUtilities.invokeLater(() -> {
                        String playerToMove = parseValue(message, "PLAYERTOMOVE");
                        String opponent = parseValue(message, "OPPONENT");

                        board = new TicTacToeGame(client);
                        board.setVisible(true);

                        if (playerToMove != null && playerToMove.equalsIgnoreCase(client.getPlayerName())) {
                            board.enablePlayerTurn();
                        }

                        statusLabel.setText("Match started with " + opponent);
                        System.out.println("DEBUG: Match started with " + opponent + ", first to move: " + playerToMove);
                    });
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
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
