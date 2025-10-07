package classes;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameModeSelector extends JFrame {

    private final GameClient client;
    private final String gameName;
    private boolean playerVsServer = false;

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

        vsPlayerBtn.addActionListener(e -> {
            playerVsServer = true;

            client.setServerListener(message -> {
                if (message.contains("SVR GAME MATCH")) {
                    SwingUtilities.invokeLater(() -> {
                        TicTacToeGame board = new TicTacToeGame(client);
                        board.setVisible(true);

                        String playerToMove = parseValue(message, "PLAYERTOMOVE");
                        if (playerToMove != null && playerToMove.equalsIgnoreCase(client.getPlayerName())) {
                            board.enablePlayerTurn();
                        }
                    });
                } else if (message.contains("SVR GAME YOURTURN")) {
                    SwingUtilities.invokeLater(() -> {
                    });
                }
            });

            try {
                client.subscribe(gameName);
                System.out.println("DEBUG: Subscribed to " + gameName);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Kan niet inschrijven: " + ex.getMessage());
            }

            dispose();
        });

        vsAIBtn.addActionListener(e -> {
            TicTacToeGame board = new TicTacToeGame(null); // no server
            board.setAIMode(true);
            board.setVisible(true);
            dispose();
        });

        setVisible(true);
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
