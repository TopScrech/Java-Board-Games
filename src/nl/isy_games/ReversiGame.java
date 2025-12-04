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

                final int row = r;
                final int col = c;

                JButton btn = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setColor(new Color(44, 44, 46));
                        g2.fillRect(0, 0, getWidth(), getHeight());

                        g2.setColor(new Color(20, 20, 20, 120));
                        g2.setStroke(new BasicStroke(1));
                        g2.drawRect(0, 0, getWidth()-1, getHeight()-1);

                        String val = board[row][col];

                        int padding = Math.max(8, Math.min(getWidth(), getHeight()) / 8);
                        int diameter = Math.min(getWidth(), getHeight()) - padding *2;
                        int x = (getWidth() - diameter) / 2;
                        int y = (getHeight() - diameter) / 2;

                        if (val.equals("X")) {
                            g2.setColor(new Color (0, 0, 0, 100));
                            g2.fillOval(x+2, y+2, diameter, diameter);

                            g2.setColor(Color.BLACK);
                            g2.fillOval(x, y, diameter, diameter);

                            g2.setColor(new Color(60, 60, 60));
                            g2.setStroke(new BasicStroke(2));
                            g2.drawOval(x, y, diameter, diameter);

                        } else if (val.equals("O")) {
                            g2.setColor(new Color( 0, 0, 0, 70 ));
                            g2.fillOval(x+2, y+2, diameter, diameter);

                            g2.setColor(Color.WHITE);
                            g2.fillOval(x, y, diameter, diameter);

                            g2.setColor(new Color(180, 180, 180));
                            g2.setStroke(new BasicStroke(2));
                            g2.drawOval(x, y, diameter, diameter);
                    }
                        g2.dispose();
                    }

                };

                btn.setPreferredSize(new Dimension(cellSize, cellSize));
                btn.setBackground(new Color(34, 139, 34));
                btn.setFocusPainted(false);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(new Color(20, 20, 20)));
                btn.setContentAreaFilled(false);
                btn.addActionListener(e -> handleCellClick(row, col));

                cells[r][c] = btn;
                add(btn);
            }
        }
    }

    private void handleCellClick(int row, int col) {
    System.out.println("Klik op: " + row + ", " + col);

    if (currentTurn == Turn.REMOTE) {
        System.out.println("Niet jouw beurt!");
        return;
    }

    ArrayList<int[]> legal = rules.getLegalMoves(board, mySymbol);

    boolean isLegal = false;
    for (int[] move : legal) {
        if (move[0] == row && move[1] == col) {
            isLegal = true;
            break;
        }
    }

    if (!isLegal) {
        System.out.println("Ongeldige zet");
        return;
    }

    applyMove(row, col, mySymbol);

    if (client != null) {
        int index = row * 8 + col;
        client.sendMove(index);
    }

    currentTurn = Turn.REMOTE;
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
        if (cells[r][c] != null) {
            cells[r][c].repaint();
        }
    }


    @Override
    public boolean isCellEmpty(int row, int col) {
        return board[row][col].equals("");
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

    @Override
    public void updateBoardFromServer(String message) {
        if (message.contains("SVR GAME MOVE")) {
            try {
                String moveIndexStr = message.replaceAll("[^0-9]", "");
                int index = Integer.parseInt(moveIndexStr);

                int row = index / 8;
                int col = index % 8;

                applyMove(row, col, opponentSymbol);

                currentTurn = Turn.LOCAL;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if (message.contains("SVR GAME YOURTURN")) {
            currentTurn = Turn.LOCAL;
            System.out.println("Jij bent aan zet!");
        }
    }

    public void setCloseCallback(Runnable callback) {
    }

    public void applyMove(int row, int col, String symbol) {
        setCell(row, col, symbol);

        ArrayList<int[]> flips = rules.checkAllDirections(board, row, col, symbol);
        for (int[] pos : flips) {
            setCell(pos[0], pos[1], symbol);
        }

        repaint();
    }


    public boolean hasLegalMove(String symbol) {
        return rules.getLegalMoves(board, symbol).size() > 0;
    }


    public void checkGameOver() {
    }

    public int[] getScore() {
        return new int[]{0, 0};
    }
}
