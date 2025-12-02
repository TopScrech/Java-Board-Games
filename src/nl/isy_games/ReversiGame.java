package nl.isy_games;

import javax.swing.*;

import nl.isy_games.TicTacToeGame.Turn;

import java.awt.*;
import java.util.ArrayList;

public class ReversiGame extends BoardGame {

    private final JButton[][] cells;
    private final int cellSize = 80;
    private GameClient client;
    private boolean myTurnFirst;
    private ReversiRules rules = new ReversiRules();

    private String mySymbol;
    private String opponentSymbol;
    private String piece;

    private Turn currentTurn;

    public ReversiGame(GameClient client, boolean myTurnFirst) {
        super(8, 8);
        this.client = client;
        this.myTurnFirst = myTurnFirst;
        if(myTurnFirst){
            this.mySymbol = "X";
            this.opponentSymbol = "O";
        }else{
            this.mySymbol = "O";
            this.opponentSymbol = "X";
        }   
        
        if (client == null) {
            this.currentTurn = myTurnFirst ? Turn.PLAYER : Turn.OPPONENT;
        } else {
            this.currentTurn = myTurnFirst ? Turn.LOCAL : Turn.REMOTE;
        }

       setPiece();
        
        cells = new JButton[rows][cols];
        setLayout(new GridLayout(rows, cols, 2, 2));
        setBackground(new Color(28, 28, 30));

        buildBoard();
        setupInitialPieces();
        setVisible(true);
    }

    public ReversiGame() {
        super(8, 8);
        cells = new JButton[rows][cols];
        setLayout(new GridLayout(rows, cols, 2, 2));
        setBackground(new Color(28, 28, 30));
        buildBoard();
        setupInitialPieces();
        setVisible(true);
        //setCell(4, 5, "X");
        //ArrayList<int[]> tiles = rules.checkAllDirections(board,4,5,"X");
        //for(int[] e : tiles){
            //setCell(e[0], e[1], "X");
        //}
        //setCell(2, 2, "X");
        //ArrayList<int[]> tiles2 = rules.checkAllDirections(board,2,2,"X");
        //for(int[] e : tiles2){
            //setCell(e[0], e[1], "X");
        //}
        ArrayList<int[]> legalMoves = rules.getLegalMoves(board, "O");
        for(int[] e : legalMoves){
            JButton btn = cells[e[0]][e[1]];
            btn.setBackground(new Color(0,255,0));
        }
        //rules.hasLegalMove(legalMoves.size());
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

    private void updateUICell(int r, int c) {
        String val = board[r][c];
        JButton btn = cells[r][c];
        btn.setText(val);
        if (val.equals("X")) btn.setForeground(Color.BLACK);
        else if (val.equals("O")) btn.setForeground(Color.WHITE);
        else btn.setForeground(Color.BLACK);
    }

    @Override
    public boolean isCellEmpty(int row, int col) {
        return true;
    }

    @Override
    public void setCell(int row, int col, String value) {
        board[row][col] = value;
        updateUICell(row, col);
    }

    public void setPiece(){
        if(currentTurn == Turn.PLAYER){
            piece = mySymbol;
        }else{
            piece = opponentSymbol;
        }
    }

    @Override
    public String[][] getBoardState() {
        return board;
    }

    public void setTurn(Turn t) {
        this.currentTurn = t;
    }

    public void updateBoardFromServer(String message) {
    }

    public void setCloseCallback(Runnable callback) {
    }
}
