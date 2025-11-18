package nl.isy_games;

import javax.swing.*;
import java.awt.*;

public class ReversiGame extends BoardGame {

    private final JButton[][] cells;
    private final int cellSize = 80;

    public ReversiGame() {
        super(8, 8);
        cells = new JButton[rows][cols];

        setLayout(new GridLayout(rows, cols, 2, 2));
        setBackground(new Color(28, 28, 30));

        buildBoard();
        setupInitialPieces();

        setVisible(true);
    }

    private void buildBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(cellSize, cellSize));
                btn.setBackground(new Color(28, 28, 30));
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                cells[r][c] = btn;
                add(btn);
            }
        }
    }

    private void setupInitialPieces() {
        setCell(3, 3, "O");
        setCell(3, 4, "X");
        setCell(4, 3, "X");
        setCell(4, 4, "O");

        updateUICell(3, 3);
        updateUICell(3, 4);
        updateUICell(4, 3);
        updateUICell(4, 4);
    }

    @Override
    public boolean isCellEmpty(int row, int col) {
        return board[row][col].isEmpty();
    }

    @Override
    public void setCell(int row, int col, String value) {
        board[row][col] = value;
        updateUICell(row, col);
    }

    private void updateUICell(int r, int c) {
        String val = board[r][c];

        JButton btn = cells[r][c];
        btn.setText(val);

        if (val.equals("X")) {
            btn.setForeground(Color.BLACK);
        } else if (val.equals("O")) {
            btn.setForeground(Color.WHITE);
        } else {
            btn.setForeground(Color.BLACK);
        }
    }

    @Override
    public String[][] getBoardState() {
        return super.getBoardState();
    }
}
