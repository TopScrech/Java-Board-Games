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

        boolean isReversi = gameName != null
                && (gameName.equalsIgnoreCase("reversi") || gameName.equalsIgnoreCase("othello"));

        setTitle("Select Game Mode");
        setSize(300, isReversi ? 270 : 190);
        setLayout(new GridLayout(isReversi ? 6 : 4, 1, 5, 5));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton vsPlayerBtn = new JButton("Spelen tegen speler (Server)");
        JButton vsAIBtn = new JButton("Spelen tegen AI (Client)");
        JButton vsRandomAIBtn = new JButton("AI vs Random");
        JButton vsTimedVsFixedBtn = isReversi ? new JButton("Timed vs Fixed (AI)") : null;
        JButton vsWijmarTimedBtn = isReversi ? new JButton("Wijmar vs Timed (AI)") : null;
        add(vsPlayerBtn);
        add(vsAIBtn);
        add(vsRandomAIBtn);
        if (vsTimedVsFixedBtn != null) {
            add(vsTimedVsFixedBtn);
        }
        if (vsWijmarTimedBtn != null) {
            add(vsWijmarTimedBtn);
        }

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        add(statusLabel);

        vsPlayerBtn.addActionListener(e -> showRandomOrFindMenu());
        vsAIBtn.addActionListener(e -> startAIMode());
        vsRandomAIBtn.addActionListener(e -> startAIVsRandomMode());
        if (vsTimedVsFixedBtn != null) {
            vsTimedVsFixedBtn.addActionListener(e -> startTimedVsFixedMode());
        }
        if (vsWijmarTimedBtn != null) {
            vsWijmarTimedBtn.addActionListener(e -> startWijmarVsTimedMode());
        }

        setVisible(true);
    }

    private void startAIMode() {
        String type = gameName == null ? "tic-tac-toe" : gameName;

        BoardGame board;
        String title;
        switch (type.toLowerCase()) {
            case "tic-tac-toe":
                board = new TicTacToeGame(null, true, true);
                title = "Tic-Tac-Toe - AI Mode";
                break;
            case "reversi":
            case "othello":
                board = new ReversiGame(null, true, true);
                title = "Reversi - AI Mode";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Game not implemented yet.");
                return;
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(board);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);

        board.setCloseCallback(() -> SwingUtilities.invokeLater(frame::dispose));

        dispose();
    }

    private void startAIVsRandomMode() {
        String type = gameName == null ? "tic-tac-toe" : gameName;

        BoardGame board;
        String title;
        switch (type.toLowerCase()) {
            case "tic-tac-toe":
                board = new TicTacToeGame(null, true,
                        symbol -> new AI("Bot", symbol),
                        symbol -> new TicTacToeRandomAI("Random", symbol));
                title = "Tic-Tac-Toe - AI vs Random";
                break;
            case "reversi":
            case "othello":
                board = new ReversiGame(null, true,
                        ReversiAISettings::createAi,
                        symbol -> new ReversiAI("Random", symbol));
                title = "Reversi - AI vs Random";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Game not implemented yet.");
                return;
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(board);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);

        board.setCloseCallback(() -> SwingUtilities.invokeLater(frame::dispose));

        dispose();
    }

    private void startTimedVsFixedMode() {
        String type = gameName == null ? "tic-tac-toe" : gameName;

        BoardGame board;
        String title;
        switch (type.toLowerCase()) {
            case "reversi":
            case "othello":
                boolean timedStarts = Math.random() < 0.5;
                board = new ReversiGame(null, timedStarts,
                        symbol -> new ReversiTimedAI("Timed", symbol, ReversiAISettings.DEFAULT_TIME_LIMIT_SECONDS),
                        symbol -> new ReversiFixedDepthAI("Fixed", symbol, ReversiAISettings.DEFAULT_FIXED_DEPTH));
                title = "Reversi - Timed vs Fixed";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Mode not available for this game.");
                return;
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(board);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);

        board.setCloseCallback(() -> SwingUtilities.invokeLater(frame::dispose));

        dispose();
    }

    private void startWijmarVsTimedMode() {
        String type = gameName == null ? "tic-tac-toe" : gameName;

        BoardGame board;
        String title;
        switch (type.toLowerCase()) {
            case "reversi":
            case "othello":
                boolean wijmarStarts = Math.random() < 0.5;
                board = new ReversiGame(null, wijmarStarts,
                        symbol -> new ReversiWijmarUltimateAI("Wijmar", symbol, ReversiAISettings.DEFAULT_TIME_LIMIT_SECONDS),
                        symbol -> new ReversiTimedAI("Timed", symbol, ReversiAISettings.DEFAULT_TIME_LIMIT_SECONDS));
                title = "Reversi - Wijmar vs Timed";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Mode not available for this game.");
                return;
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(board);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);

        board.setCloseCallback(() -> SwingUtilities.invokeLater(frame::dispose));

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
