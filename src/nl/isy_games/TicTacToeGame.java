package nl.isy_games;

import javax.swing.*;
import java.awt.*;

public class TicTacToeGame extends JFrame {

    private JButton[][] cells;
    private boolean playerTurn = true;
    private boolean aiMode = false;
    private boolean gameOver = false;
    private GameClient client;
    private AI aiPlayer;
    private int rows = 3;
    private int cols = 3;
    private String gameType = "tic-tac-toe";

    public TicTacToeGame(GameClient client) {
        this(client, "tic-tac-toe");
    }

    public TicTacToeGame(GameClient client, String gameType) {
        this.client = client;
        this.gameType = gameType.toLowerCase();

        int[] size = getBoardSize(gameType);
        this.rows = size[0];
        this.cols = size[1];
        this.cells = new JButton[rows][cols];

        setTitle("Game: " + gameType);
        setSize(80 * cols, 80 * rows);
        setLayout(new GridLayout(rows, cols));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildBoard();

        setVisible(true);
    }

    private int[] getBoardSize(String gameType) {
        switch (gameType.toLowerCase()) {
            case "tic-tac-toe":
                return new int[]{3, 3};
            case "reversi":
            case "othello":
                return new int[]{8, 8};
            case "connect-four":
            case "connect4":
                return new int[]{6, 7};
            default:
                System.out.println("Unknown game type: " + gameType + ", defaulting to 3x3");
                return new int[]{3, 3};
        }
    }

    private void buildBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = new JButton("");
                btn.setFont(new Font("Arial", Font.BOLD, 30));
                final int row = r, col = c;
                btn.addActionListener(e -> handleCellClick(row, col));
                cells[r][c] = btn;
                add(btn);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (gameOver || !isCellEmpty(row, col)) return;

        if (!playerTurn) {
            JOptionPane.showMessageDialog(this, "Not your turn!");
            return;
        }

        cells[row][col].setText("X");
        playerTurn = false;

        if (gameType.equals("tic-tac-toe")) {
            checkGameOver("X");
        }

        if (!gameOver) {
            if (aiMode) aiMove();
            else if (client != null) {
                int moveIndex = row * cols + col;
                client.sendMove(moveIndex);
            }
        }
    }

    private void aiMove() {
        if (gameOver) return;

        int[] move = aiPlayer.chooseMove(this);
        if (move == null) return;

        cells[move[0]][move[1]].setText("O");
        if (gameType.equals("tic-tac-toe")) {
            checkGameOver("O");
        }
        playerTurn = true;
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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isCellEmpty(r, c)) {
                    allFilled = false;
                    break;
                }
            }
        }

        if (allFilled) {
            gameOver = true;
            JOptionPane.showMessageDialog(this, "Draw!");
        }
    }

    public boolean isCellEmpty(int r, int c) {
        return cells[r][c].getText().isEmpty();
    }

    public void enablePlayerTurn() {
        if (!gameOver) playerTurn = true;
    }

    public void setAIMode(boolean aiMode) {
        this.aiMode = aiMode;
        if (aiMode) {
            aiPlayer = new AI("Computer", "O");
            enablePlayerTurn();
        }
    }
}
