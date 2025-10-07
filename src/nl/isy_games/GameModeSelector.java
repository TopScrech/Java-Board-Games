package classes;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameModeSelector extends JFrame {

    private final GameClient client;
    private final String gameName;
    private JLabel statusLabel;

    private enum MatchMode { NONE, RANDOM, FIND_PLAYER }
    private MatchMode currentMode = MatchMode.NONE;
    private TicTacToeGame board = null;
    private String currentOpponent = null;
    private final java.util.Set<Integer> handledChallenges = new java.util.HashSet<>(); // track handled challenges

    public GameModeSelector(GameClient client, String gameName) {
        this.client = client;
        this.gameName = gameName;

        setTitle("Select Game Mode");
        setSize(300, 150);
        setLayout(new GridLayout(3, 1, 5, 5));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel label = new JLabel("Kies modus voor " + gameName + ":", SwingConstants.CENTER);
        add(label);

        JButton vsPlayerBtn = new JButton("Spelen tegen speler (Server)");
        JButton vsAIBtn = new JButton("Spelen tegen AI (Client)");
        add(vsPlayerBtn);
        add(vsAIBtn);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        add(statusLabel);

        vsPlayerBtn.addActionListener(e -> showRandomOrFindMenu());
        vsAIBtn.addActionListener(e -> startAIMode());

        setupServerListener();

        setVisible(true);
    }

    private void startAIMode() {
        board = new TicTacToeGame(null);
        board.setAIMode(true);
        board.setVisible(true);
        dispose();
    }

    private void setupServerListener() {
        client.addServerListener(message -> {
            if (message.contains("SVR GAME CHALLENGE")) {
                String challenger = parseValue(message, "CHALLENGER");
                String challengeNumberStr = parseValue(message, "CHALLENGENUMBER");
                if (challenger == null || challengeNumberStr == null) return;

                int challengeNumber = Integer.parseInt(challengeNumberStr);
                if (handledChallenges.contains(challengeNumber)) return;
                handledChallenges.add(challengeNumber);

                if (currentMode == MatchMode.RANDOM) {
                    System.out.println("DEBUG: Random mode auto-accept challenge from " + challenger);
                    client.acceptChallenge(challengeNumber);
                    return;
                }

                if (currentMode == MatchMode.FIND_PLAYER) {
                    SwingUtilities.invokeLater(() -> {
                        int response = JOptionPane.showConfirmDialog(
                                this,
                                challenger + " has challenged you to " + gameName + ". Accept?",
                                "Incoming Challenge",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (response == JOptionPane.YES_OPTION) {
                            client.acceptChallenge(challengeNumber);
                            statusLabel.setText("Accepted challenge from " + challenger);
                        } else {
                            client.denyChallenge(challengeNumber);
                            statusLabel.setText("Denied challenge from " + challenger);
                        }
                    });
                }
            }

            if (message.contains("SVR GAME MATCH")) {
                String opponent = parseValue(message, "OPPONENT");
                SwingUtilities.invokeLater(() -> {
                    if (board == null || !board.isDisplayable()) {
                        board = new TicTacToeGame(client);
                        board.setVisible(true);
                        statusLabel.setText("Match started with " + opponent);
                        dispose();
                    } else {
                        System.out.println("DEBUG: Board already open, skipping duplicate.");
                    }
                });
            }
        });
    }

    private void showRandomOrFindMenu() {
        JFrame selectFrame = new JFrame("Choose Match Type");
        selectFrame.setSize(300, 150);
        selectFrame.setLayout(new FlowLayout());
        selectFrame.setLocationRelativeTo(this);
        selectFrame.setAlwaysOnTop(true);
        selectFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton randomBtn = new JButton("Random");
        JButton findBtn = new JButton("Find Player");

        selectFrame.add(randomBtn);
        selectFrame.add(findBtn);
        selectFrame.setVisible(true);

        randomBtn.addActionListener(e -> {
            selectFrame.dispose();
            startPvPRandom();
        });

        findBtn.addActionListener(e -> {
            selectFrame.dispose();
            startPvPFindPlayer();
        });
    }

    private void startPvPRandom() {
        currentMode = MatchMode.RANDOM;
        statusLabel.setText("Searching for random opponent...");
        client.subscribe(gameName);

        new Thread(() -> {
            try {
                boolean challengeSent = false;
                while (!challengeSent) {
                    List<String> players = client.getPlayerList();
                    for (String p : players) {
                        if (!p.equalsIgnoreCase(client.getPlayerName())) {
                            // Only send challenge if our name is lexicographically smaller
                            if (client.getPlayerName().compareToIgnoreCase(p) < 0) {
                                System.out.println("DEBUG: Found random opponent: " + p + ", sending challenge");
                                client.challenge(p, gameName);
                            } else {
                                System.out.println("DEBUG: Found random opponent: " + p + ", waiting for their challenge");
                            }
                            challengeSent = true; // stop the loop
                            break;
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void startPvPFindPlayer() {
        currentMode = MatchMode.FIND_PLAYER;
        String opponentName = JOptionPane.showInputDialog(this, "Enter player name:");
        if (opponentName == null || opponentName.isEmpty()) return;

        try {
            List<String> players = client.getPlayerList();
            if (!players.contains(opponentName)) {
                JOptionPane.showMessageDialog(this, "No player found.");
                showRandomOrFindMenu();
                return;
            }

            client.challenge(opponentName, gameName);
            statusLabel.setText("Invite sent to " + opponentName);

        } catch (Exception e) {
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
