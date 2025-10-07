package classes;

import javax.swing.*;
import java.awt.*;

public class TicTacToeGame extends JFrame {

    private JButton[][] cells = new JButton[3][3];
    private boolean playerTurn = true;
    private boolean aiMode = false;
    private GameClient client; // null if AI mode
    private boolean gameOver = false;
    private AI aiPlayer;

    public TicTacToeGame(GameClient client) {
        this.client = client;

        setTitle("TicTacToe");
        setSize(300, 300);
        setLayout(new GridLayout(3, 3));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                JButton btn = new JButton("");
                btn.setFont(new Font("Arial", Font.BOLD, 40));
                final int r = row, c = col;
                btn.addActionListener(e -> playerMove(r, c));
                cells[row][col] = btn;
                add(btn);
            }
        }

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void setAIMode(boolean aiMode) {
        this.aiMode = aiMode;
        if (aiMode) {
            aiPlayer = new AI("Computer", "O");
            enablePlayerTurn();
        }
    }

    public void enablePlayerTurn() {
        if (!gameOver) playerTurn = true;
    }

    private void playerMove(int row, int col) {
        if (!playerTurn || !isCellEmpty(row, col) || gameOver) return;

        cells[row][col].setText("X");
        playerTurn = false;

        checkGameOver("X");

        if (!gameOver) {
            if (aiMode) {
                aiMove();
            } else if (client != null) {
                int moveIndex = row * 3 + col;
                client.sendMove(moveIndex);
            }
        }
    }

    private void aiMove() {
        if (gameOver) return;

        int[] move = aiPlayer.chooseMove(this);
        cells[move[0]][move[1]].setText("O");

        checkGameOver("O");
        playerTurn = true;
    }

    public void updateBoardFromServer(String message) {
        if (gameOver) return;

        try {
            String moveStr = message.split("MOVE:")[1].trim();
            int move = Integer.parseInt(moveStr);
            int row = move / 3;
            int col = move % 3;
            cells[row][col].setText("O");
            checkGameOver("O");
            enablePlayerTurn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCellEmpty(int row, int col) {
        return cells[row][col].getText().equals("");
    }

    private void checkGameOver(String playerSymbol) {
        int[][] winCombos = {
                {0,0,0,1,0,2}, {1,0,1,1,1,2}, {2,0,2,1,2,2}, // rows
                {0,0,1,0,2,0}, {0,1,1,1,2,1}, {0,2,1,2,2,2}, // columns
                {0,0,1,1,2,2}, {0,2,1,1,2,0}                 // diagonals
        };

        for (int[] combo : winCombos) {
            if (cells[combo[0]][combo[1]].getText().equals(playerSymbol) &&
                    cells[combo[2]][combo[3]].getText().equals(playerSymbol) &&
                    cells[combo[4]][combo[5]].getText().equals(playerSymbol)) {

                gameOver = true;
                JOptionPane.showMessageDialog(this,
                        "Speler " + playerSymbol + " wint!");
                return;
            }
        }

        // Check draw
        boolean allFilled = true;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (cells[row][col].getText().equals("")) {
                    allFilled = false;
                    break;
                }
            }
        }
        if (allFilled && !gameOver) {
            gameOver = true;
            JOptionPane.showMessageDialog(this, "Gelijkspel!");
        }
    }
}
