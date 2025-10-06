package nl.isy_games;

import nl.isy_games.Player;
import nl.isy_games.TicTacToeGame;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameSelector extends JFrame {

    private final classes.GameClient client;
    private TicTacToeGame game;
    private Player me, opponent;
    private Player currentPlayer;
    private JFrame gameFrame;
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel turnLabel;

    private boolean boardOpened = false;

    public GameSelector(classes.GameClient client) {
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

                button.addActionListener(e -> {
                    if (gameName.equalsIgnoreCase("Tic-tac-toe")) {
                        try {
                            client.subscribe("Tic-tac-toe");

                            if (statusLabel == null) {
                                statusLabel = new JLabel("Ingeschreven voor " + gameName + " - Wachten op tegenstander...",
                                        SwingConstants.CENTER);
                                add(statusLabel);
                                revalidate();
                                repaint();
                            }

                            new Thread(() -> {
                                try {
                                    client.listen(message -> SwingUtilities.invokeLater(() -> handleServerMessage(message)));
                                } catch (IOException ex) {
                                    SwingUtilities.invokeLater(() ->
                                            JOptionPane.showMessageDialog(this,
                                                    "Fout bij luisteren naar server: " + ex.getMessage()));
                                }
                            }).start();

                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this,
                                    "Kan niet inschrijven: " + ex.getMessage());
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Het spel '" + gameName + "' is nog niet beschikbaar.");
                    }
                });
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Kon geen gamelist ophalen: " + e.getMessage());
        }

        setVisible(true);
    }

    private void updateTurnLabel() {
        if (currentPlayer == me) {
            turnLabel.setText("Jouw beurt (" + me.getMark() + ")");
        } else {
            turnLabel.setText("Beurt van " + opponent.getName() + " (" + opponent.getMark() + ")");
        }
    }


    private void handleServerMessage(String message) {
        System.out.println("Server: " + message);

        if (message.startsWith("SVR GAME MATCH")) {

            String myName = client.getPlayerName();
            String playerToMove = extractValue(message, "PLAYERTOMOVE");
            String opponentName = extractValue(message, "OPPONENT");

            if (opponentName == null || opponentName.isEmpty() || opponentName.equalsIgnoreCase(myName)) {
                System.out.println("WARNING: Ignoring invalid match: opponent='" + opponentName + "'");
                return;
            }

            if (statusLabel != null) {
                remove(statusLabel);
                statusLabel = null;
                revalidate();
                repaint();
            }

            if (playerToMove.equals(myName)) {
                me = new Player(myName, "X");
                opponent = new Player(opponentName, "O");
                currentPlayer = me;
            } else {
                me = new Player(myName, "O");
                opponent = new Player(opponentName, "X");
                currentPlayer = opponent;
            }

            game = new TicTacToeGame();
            game.setPlayers(me, opponent);

            System.out.println("DEBUG: Opening JFrame for player '" + me.getName() + "' vs opponent '" + opponent.getName() + "'");

            String gameType = extractValue(message, "GAMETYPE");
            setupBoard(gameType);
        }

        else if (message.startsWith("SVR GAME MOVE")) {
            if (game == null) return;

            String[] parts = message.replace("{", "").replace("}", "").split(",");
            String playerName = parts[0].split(":")[1].replace("\"", "").trim();
            int row = Integer.parseInt(parts[1].split(":")[1].trim());
            int col = Integer.parseInt(parts[2].split(":")[1].trim());

            Player player = playerName.equals(me.getName()) ? me : opponent;
            game.makeMove(player, row, col);
            buttons[row][col].setText(player.getMark());
            currentPlayer = (player == me) ? opponent : me;

            updateTurnLabel();
        }

        else if (message.startsWith("SVR GAME WIN") ||
                message.startsWith("SVR GAME LOSS") ||
                message.startsWith("SVR GAME DRAW")) {
            JOptionPane.showMessageDialog(this, "Match einde: " + message);
            if (gameFrame != null) {
                gameFrame.dispose();
                gameFrame = null;
            }
        }
    }

    private String extractValue(String msg, String key) {
        int start = msg.indexOf(key + ":");
        if (start == -1) return "";
        start = msg.indexOf("\"", start) + 1;
        int end = msg.indexOf("\"", start);
        return msg.substring(start, end);
    }

    private void setupBoard(String gameType) {
        gameFrame = new JFrame(gameType + ": " + me.getName() + " vs " + opponent.getName());
        gameFrame.setSize(400, 450);
        gameFrame.setLayout(new BorderLayout());
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        turnLabel = new JLabel("", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gameFrame.add(turnLabel, BorderLayout.NORTH);

        JPanel boardPanel;
        int rows, cols;

        switch (gameType.toLowerCase()) {
            case "tic-tac-toe":
                rows = cols = 4;
                break;
            case "connect4":
                rows = 6;
                cols = 7;
                break;
            case "battleship":
                rows = cols = 10;
                break;
            default:
                JOptionPane.showMessageDialog(this,
                        "Het spel '" + gameType + "' wordt nog niet ondersteund.");
                return;
        }

        boardPanel = new JPanel(new GridLayout(rows, cols));
        buttons = new JButton[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = new JButton("");
                buttons[r][c] = btn;
                int row = r, col = c;

                btn.addActionListener(e -> {
                    if (currentPlayer == me) {
                        try {
                            if (gameType.equalsIgnoreCase("tic-tac-toe")) {
                                if (game.isCellEmpty(row, col)) {
                                    client.sendMove(row * cols + col);
                                    game.makeMove(me, row, col);
                                    btn.setText(me.getMark());
                                    currentPlayer = opponent;
                                    updateTurnLabel();
                                }
                            } else if (gameType.equalsIgnoreCase("connect4")) {
                                System.out.println("Niet beschikbaar");
                            } else if (gameType.equalsIgnoreCase("battleship")) {
                                System.out.println("Niet beschikbaar");
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(gameFrame,
                                    "Fout bij verzenden van zet: " + ex.getMessage());
                        }
                    }
                });
                boardPanel.add(btn);
            }
        }

        gameFrame.add(boardPanel, BorderLayout.CENTER);
        gameFrame.setLocationRelativeTo(null);

        updateTurnLabel();
        gameFrame.setVisible(true);
    }

}
