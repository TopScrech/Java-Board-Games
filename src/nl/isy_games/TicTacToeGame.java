package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TicTacToeGame extends JFrame {

    private final GameClient client;
    private final JButton[][] buttons = new JButton[3][3];
    private boolean myTurn = false;  
    private String myMark;          
    private String opponentMark;

    public TicTacToeGame(GameClient client) {
        this.client = client;

        setTitle("TicTacToe - Match");
        setSize(400, 400);
        setLayout(new GridLayout(3, 3));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initBoard();
    }

    private void initBoard() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton button = new JButton("");
                button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
                buttons[r][c] = button;

                int row = r;
                int col = c;

                button.addActionListener(e -> {
                    if (myTurn && button.getText().isEmpty()) {
                        makeMove(row, col);
                    }
                });

                add(button);
            }
        }
    }

    private void makeMove(int row, int col) {
        buttons[row][col].setText(myMark);
        buttons[row][col].setEnabled(false);

        int moveIndex = row * 3 + col;
        client.sendMove(moveIndex);

        myTurn = false;
    }

    public void enablePlayerTurn() {
        myTurn = true;
    }

    public void updateBoardFromServer(String message) {
        String moveStr = parseValue(message, "MOVE");
        String player = parseValue(message, "PLAYER");

        if (moveStr != null && player != null && !player.equalsIgnoreCase(client.getPlayerName())) {
            int move = Integer.parseInt(moveStr);
            int row = move / 3;
            int col = move % 3;
            buttons[row][col].setText(opponentMark);
            buttons[row][col].setEnabled(false);
            myTurn = true;  
        }
    }

    public void setMarks(String myMark) {
        this.myMark = myMark;
        this.opponentMark = myMark.equals("X") ? "O" : "X";
    }

    private String parseValue(String message, String key) {
        try {
            int start = message.indexOf(key + ": \"");
            if (start < 0) return null;
            start += key.length() + 3;
            int end = message.indexOf("\"", start);
            if (end < 0) return null;
            return message.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}

