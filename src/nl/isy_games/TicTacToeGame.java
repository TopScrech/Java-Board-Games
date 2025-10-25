package nl.isy_games;

import javax.swing.*;
import java.awt.*;

public class TicTacToeGame extends JPanel {

    private JButton[][] cells;
    private boolean aiMode = false;
    private boolean gameOver = false;
    private GameClient client;
    private AI aiPlayer;
    private int rows = 3;
    private int cols = 3;
    private String gameType = "tic-tac-toe";

    private enum Turn { PLAYER, OPPONENT }
    private Turn currentTurn = Turn.PLAYER;

    private String mySymbol = "X";
    private String opponentSymbol = "O";

    private JLabel turnLabel;

    public TicTacToeGame(String gameType) {
        this(null, gameType, "X", "O", true);
        setAIMode(true);
    }

    public TicTacToeGame(GameClient client, String gameType, String mySymbol, String opponentSymbol, boolean myTurnFirst) {
        this.client = client;
        this.gameType = gameType.toLowerCase();
        this.mySymbol = mySymbol;
        this.opponentSymbol = opponentSymbol;
        this.currentTurn = myTurnFirst ? Turn.PLAYER : Turn.OPPONENT;

        int[] size = getBoardSize(gameType);
        this.rows = size[0];
        this.cols = size[1];
        this.cells = new JButton[rows][cols];

        turnLabel = new JLabel("", SwingConstants.CENTER);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel boardPanel = new JPanel(new GridLayout(rows, cols, 5, 5));
        boardPanel.setBackground(new Color(28, 28, 30));
        buildBoard(boardPanel);

        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.setPreferredSize(new Dimension(455, 455));
        boardWrapper.setBackground(new Color(28, 28, 30));
        boardWrapper.add(boardPanel);

        setLayout(new BorderLayout());
        setBackground(new Color(28, 28, 30));
        add(turnLabel, BorderLayout.NORTH);
        add(boardWrapper, BorderLayout.CENTER);

        updateTurnLabel();
        setVisible(true);
    }


    private int[] getBoardSize(String gameType) {
        switch (gameType.toLowerCase()) {
            case "tic-tac-toe": return new int[]{3, 3};
            case "reversi":
            case "othello": return new int[]{8, 8};
            case "connect-four":
            case "connect4": return new int[]{6, 7};
            default:
                System.out.println("Unknown game type: " + gameType + ", defaulting to 3x3");
                return new int[]{3, 3};
        }
    }

    private void buildBoard(JPanel boardPanel) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createRoundedButton("");
                btn.setPreferredSize(new Dimension(150, 150));
                final int row = r, col = c;
                btn.addActionListener(e -> {
                    handleCellClick(row, col);
                    updateTurnLabel();
                });
                cells[r][c] = btn;
                boardPanel.add(btn);
            }
        }
    }

    private void updateTurnLabel() {
        if (gameOver) turnLabel.setText("Game over");
        else if (currentTurn == Turn.PLAYER) turnLabel.setText("Your turn: " + mySymbol);
        else turnLabel.setText("Opponent's turn: " + opponentSymbol);
    }

    private JButton createRoundedButton(String text) {
        Color buttonColor = new Color(44, 44, 46);
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? buttonColor.darker() : buttonColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFont(new Font("Arial", Font.BOLD, 60));
        button.setForeground(Color.WHITE);
        return button;
    }

    private void handleCellClick(int row, int col) {
        if (gameOver || !isCellEmpty(row, col)) return;
        if (currentTurn != Turn.PLAYER) return;

        cells[row][col].setText(mySymbol);
        cells[row][col].setForeground(new Color(0, 191, 255));

        checkGameOver(mySymbol);

        currentTurn = Turn.OPPONENT;
        SwingUtilities.invokeLater(this::updateTurnLabel);

        if (client != null && !aiMode) {
            int moveIndex = row * cols + col;
            client.sendMove(moveIndex);
        }

        if (!gameOver && aiMode) {
            new Thread(() -> {
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(this::aiMove);
            }).start();
        }
    }



    private void aiMove() {
        if (gameOver) return;

        int[] move = aiPlayer.chooseMove(this);
        if (move == null) return;

        cells[move[0]][move[1]].setText(opponentSymbol);
        cells[move[0]][move[1]].setForeground(new Color(255, 69, 0));

        checkGameOver(opponentSymbol);

        currentTurn = Turn.PLAYER;
        SwingUtilities.invokeLater(this::updateTurnLabel);
    }

    private void checkGameOver(String symbol) {
        if (!gameType.equals("tic-tac-toe")) return;

        int[][] winCombos = {
                {0,0,0,1,0,2}, {1,0,1,1,1,2}, {2,0,2,1,2,2},
                {0,0,1,0,2,0}, {0,1,1,1,2,1}, {0,2,1,2,2,2},
                {0,0,1,1,2,2}, {0,2,1,1,2,0}
        };

        for (int[] combo : winCombos) {
            if (cells[combo[0]][combo[1]].getText().equals(symbol) &&
                    cells[combo[2]][combo[3]].getText().equals(symbol) &&
                    cells[combo[4]][combo[5]].getText().equals(symbol)) {

                gameOver = true;
                JOptionPane.showMessageDialog(this, "Player " + symbol + " wins!");
                return;
            }
        }

        boolean allFilled = true;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (isCellEmpty(r, c)) { allFilled = false; break; }

        if (allFilled) {
            gameOver = true;
            JOptionPane.showMessageDialog(this, "Draw!");
        }
    }

    public boolean isCellEmpty(int r, int c) {
        return cells[r][c].getText().isEmpty();
    }

    public String[][] getBoardState() {
        String[][] state = new String[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String value = cells[r][c].getText();
                state[r][c] = value == null ? "" : value.trim();
            }
        }
        return state;
    }

    public void updateBoardFromServer(String message) {
        if (message.contains("MOVE")) {
            int moveIndex = parseMoveIndex(message);
            int row = moveIndex / cols;
            int col = moveIndex % cols;

            if (!isCellEmpty(row, col)) {
                return;
            }

            cells[row][col].setText(opponentSymbol);
            cells[row][col].setForeground(new Color(255, 69, 0));
            checkGameOver(opponentSymbol);

            currentTurn = Turn.PLAYER;
            SwingUtilities.invokeLater(this::updateTurnLabel);
        }
    }

    private int parseMoveIndex(String message) {
        int idx = message.indexOf("MOVE:");
        int start = message.indexOf("\"", idx) + 1;
        int end = message.indexOf("\"", start);
        return Integer.parseInt(message.substring(start, end).trim());
    }


    public void setAIMode(boolean aiMode) {
        this.aiMode = aiMode;
        if (aiMode) {
            aiPlayer = new AI("Computer", opponentSymbol);
            SwingUtilities.invokeLater(this::updateTurnLabel);
        }
    }
}
