package classes;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class BoardGame extends JPanel {
    protected int rows;
    protected int cols;
    protected String[][] board;

    public void initializeBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        board = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                board[r][c] = "";
    }

    public boolean isCellEmpty(int r, int c) { return board[r][c].isEmpty(); }
    public void setCell(int r, int c, String symbol) { board[r][c] = symbol; }
    public String[][] getBoardState() {
        String[][] copy = new String[rows][cols];
        for (int r=0; r<rows; r++)
            System.arraycopy(board[r], 0, copy[r], 0, cols);
        return copy;
    }
    public boolean isFull() {
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                if (board[r][c].isEmpty()) return false;
        return true;
    }
}
