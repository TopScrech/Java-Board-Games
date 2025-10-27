package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameModeSelector extends JFrame {

    private final GameClient client;
    private final String gameName;
    private final JLabel statusLabel;

    public GameModeSelector(GameClient client, String gameName) {
        this.client = client;
        this.gameName = gameName;

        setTitle("Select Game Mode");
        setSize(300, 150);
        setLayout(new GridLayout(3, 1, 5, 5));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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
        String type = gameName == null ? "tic-tac-toe" : gameName;
        TicTacToeGame board = new TicTacToeGame(type);
        board.setAIMode(true);

        JFrame frame = new JFrame("Tic-Tac-Toe - AI");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(board);
        frame.pack();
        frame.setLocationRelativeTo(this);
        board.setCloseCallback(() -> SwingUtilities.invokeLater(frame::dispose));
        frame.setVisible(true);

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
            startPvPRandom();
        });

        findBtn.addActionListener(e -> {
            selectFrame.dispose();
            startPvPFindPlayer();
        });
    }

    private void startPvPRandom() {
        MatchHandler.setMode(client, MatchHandler.Mode.RANDOM);
        statusLabel.setText("Searching for random opponent...");
        client.subscribe(gameName);

        new Thread(() -> {
            try {
                boolean challengeSent = false;
                while (!challengeSent) {
                    List<String> players = client.getPlayerList();
                    for (String p : players) {
                        if (!p.equalsIgnoreCase(client.getPlayerName())) {
                            if (client.getPlayerName().compareToIgnoreCase(p) < 0) {
                                System.out.println("DEBUG: Found random opponent: " + p + ", sending challenge");
                                client.challenge(p, gameName);
                            }
                            challengeSent = true;
                            break;
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        dispose();
    }

    private void startPvPFindPlayer() {
        MatchHandler.setMode(client, MatchHandler.Mode.FIND_PLAYER);

        String opponentName = JOptionPane.showInputDialog(this, "Enter player name:");
        if (opponentName == null || opponentName.isEmpty()) return;

        try {
            List<String> players = client.getPlayerList();
            if (!players.contains(opponentName)) {
                JOptionPane.showMessageDialog(this, "No player found.");
                return;
            }

            client.challenge(opponentName, gameName);
            statusLabel.setText("Invite sent to " + opponentName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dispose();
    }
}
