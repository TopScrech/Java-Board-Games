package classes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameSelector extends JFrame {

    private final GameClient client;
    private JLabel statusLabel;
    private boolean inMatch = false;

    private TicTacToeGame board;

    public GameSelector(GameClient client) {
        this.client = client;

        setTitle("Game Selector");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(0, 1, 10, 10));

        JLabel label = new JLabel("Kies een spel:", SwingConstants.CENTER);
        add(label);

        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                JButton button = new JButton(gameName);
                add(button);
                button.addActionListener(e -> subscribeGame(gameName));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Kon geen gamelist ophalen: " + e.getMessage());
        }

        client.setServerListener(this::handleServerMessage);

        new Thread(() -> {
            try {
                client.listen(); 
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Fout bij luisteren naar server: " + ex.getMessage()));
            }
        }).start();

        setVisible(true);
    }

    private void subscribeGame(String gameName) {
        if (!gameName.equalsIgnoreCase("tic-tac-toe")) {
            JOptionPane.showMessageDialog(this,
                    "Het spel '" + gameName + "' is nog niet beschikbaar.");
            return;
        }

        try {
            client.subscribe("tic-tac-toe");
            System.out.println("DEBUG: Subscribed to " + gameName);

            if (statusLabel == null) {
                statusLabel = new JLabel("Ingeschreven voor " + gameName + " - Wachten op tegenstander...",
                        SwingConstants.CENTER);
                add(statusLabel);
                revalidate();
                repaint();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Kan niet inschrijven: " + ex.getMessage());
        }
    }


    private void handleServerMessage(String message) {
        System.out.println("DEBUG Server message: " + message);

        SwingUtilities.invokeLater(() -> {
            if ((message.contains("SVR GAME MATCH") || message.contains("SVR GAME YOURTURN")) && !inMatch) {
                inMatch = true;

                String playerToMove = parseValue(message, "PLAYERTOMOVE");
                String opponent = parseValue(message, "OPPONENT");

                if (board == null) {
                    board = new TicTacToeGame(client);
                    board.setVisible(true);
                }

                JFrame matchFrame = new JFrame("Match Info");
                matchFrame.setSize(300, 100);
                matchFrame.setLayout(new BorderLayout());
                matchFrame.setLocationRelativeTo(this);
                JLabel label = new JLabel("Match gevonden! Je speelt tegen: " + opponent, SwingConstants.CENTER);
                matchFrame.add(label, BorderLayout.CENTER);
                matchFrame.setVisible(true);

                new Thread(() -> {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ignored) {}
                    SwingUtilities.invokeLater(matchFrame::dispose);
                }).start();

                if (playerToMove != null && playerToMove.equalsIgnoreCase(client.getPlayerName())) {
                    board.enablePlayerTurn();
                }

            } else if (message.contains("SVR GAME YOURTURN")) {
                if (board != null) board.enablePlayerTurn();

            } else if (message.contains("SVR GAME MOVE")) {
                if (board != null) board.updateBoardFromServer(message);

            } else if (message.contains("SVR GAME WIN") ||
                    message.contains("SVR GAME LOSS") ||
                    message.contains("SVR GAME DRAW")) {

                JFrame endFrame = new JFrame("Match Einde");
                endFrame.setSize(300, 100);
                endFrame.setLayout(new BorderLayout());
                endFrame.setLocationRelativeTo(this);
                JLabel label = new JLabel("Match afgelopen: " + message, SwingConstants.CENTER);
                endFrame.add(label, BorderLayout.CENTER);
                endFrame.setVisible(true);

                inMatch = false;
                if (board != null) board.dispose();
                board = null;

                new Thread(() -> {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ignored) {}
                    SwingUtilities.invokeLater(endFrame::dispose);
                }).start();
            }
        });
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
