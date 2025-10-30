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
                btn.setBackground(new Color(60, 128, 0));
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                cells[r][c] = btn;
                add(btn);
            }
        }
    }

    private void setupInitialPieces() {
        cells[3][3].setText("O");
        cells[3][4].setText("X");
        cells[4][3].setText("X");
        cells[4][4].setText("O");

        cells[3][3].setForeground(Color.WHITE);
        cells[3][4].setForeground(Color.BLACK);
        cells[4][3].setForeground(Color.BLACK);
        cells[4][4].setForeground(Color.WHITE);
    }

    @Override
    public boolean isCellEmpty(int row, int col) {
        return cells[row][col].getText().isEmpty();
    }

    @Override
    public void setCell(int row, int col, String value) {
        cells[row][col].setText(value);
    }

    @Override
    public String[][] getBoardState() {
        String[][] state = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                state[r][c] = cells[r][c].getText().trim();
        return state;
    }
}
