package classes;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.IOException;

public class GameModeSelector extends JFrame {

    private final GameClient client;
    private final String gameName;
    private boolean matchInProgress = false;
    private JLabel statusLabel;

    public GameModeSelector(GameClient client, String gameName, GameSelector gameSelector) {
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

        setVisible(true);
    }

    private void startAIMode() {
        TicTacToeGame board = new TicTacToeGame(null);
        board.setAIMode(true);
        board.setVisible(true);
        dispose();
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
            try {
                startPvPRandom(gameName);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error starting Random PvP: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        findBtn.addActionListener(e -> {
            selectFrame.dispose();
            boolean validInput = false;

            while (!validInput) {
                String opponentName = JOptionPane.showInputDialog(this, "Enter opponent name:");
                if (opponentName == null) {
                    validInput = true;
                    break;
                }
                opponentName = opponentName.trim();
                if (opponentName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid name.",
                            "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                try {
                    List<String> players = client.getPlayerList();
                    if (players.contains(opponentName)) {
                        System.out.println("DEBUG: Found opponent: " + opponentName);
                        MatchHandler.sendChallenge(client, opponentName, gameName);
                        validInput = true;
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "No player found with that name. Try again.",
                                "Player Not Found", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error fetching player list: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    validInput = true;
                }
            }
        });
    }

    public void startPvPRandom(String gameName) {
        if (matchInProgress) {
            JOptionPane.showMessageDialog(this,
                    "You are already in a match.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        matchInProgress = true;
        System.out.println("DEBUG: Starting Random PvP matchmaking for " + gameName);

        if (statusLabel != null) {
            statusLabel.setText("Searching for random opponent...");
        }

        try {
            System.out.println("DEBUG: Subscribing for Random PvP: " + gameName);
            client.subscribe(gameName);
            System.out.println("DEBUG: Subscribed to " + gameName);

            new Thread(() -> MatchHandler.startRandomMatchmaking(client, gameName, this)).start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error subscribing: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            matchInProgress = false;
            if (statusLabel != null) statusLabel.setText(" ");
        }
    }

    public boolean isMatchInProgress() {
        return matchInProgress;
    }

    public void setMatchInProgress(boolean inProgress) {
        this.matchInProgress = inProgress;
    }

    public void setStatusLabelText(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }
}
