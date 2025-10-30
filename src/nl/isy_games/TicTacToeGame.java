package nl.isy_games;

import javax.swing.*;
import java.awt.*;

public class TicTacToeGame extends BoardGame {

    public enum Turn { PLAYER, OPPONENT, LOCAL, REMOTE }

    private final JButton[][] cells;
    private boolean aiMode = false;
    private boolean gameOver = false;
    private AI aiPlayer;
    private Runnable closeCallback = () -> {};

    private Turn currentTurn;
    private final Turn initialTurn;

    private String mySymbol;
    private String opponentSymbol;

    private JLabel turnLabel;

    private final GameClient client;
    private final int cellSize = 165;

    public TicTacToeGame(GameClient client, boolean playerFirst, boolean enableAI) {
        super(3,3);
        this.client = client;
        this.mySymbol = "X";
        this.opponentSymbol = "O";
        this.currentTurn = playerFirst ? Turn.PLAYER : Turn.OPPONENT;
        this.initialTurn = this.currentTurn;
        this.cells = new JButton[rows][cols];
        this.aiMode = enableAI;

        if (aiMode) {
            aiPlayer = new AI("Bot", opponentSymbol);
        }

        initializeUI();

        if (aiMode && currentTurn == Turn.OPPONENT) {
            SwingUtilities.invokeLater(this::aiMove);
        }
    }

    public void setSymbols(String my, String opponent) {
        this.mySymbol = my;
        this.opponentSymbol = opponent;
        if (aiMode && aiPlayer != null) {
            aiPlayer = new AI("Bot", opponentSymbol); 
        }
    }

    public void setTurn(Turn t) {
        this.currentTurn = t;
        updateTurnLabel();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(28,28,30));

        turnLabel = new JLabel("", SwingConstants.CENTER);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        add(turnLabel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(rows, cols, 5,5));
        boardPanel.setBackground(new Color(28,28,30));
        buildBoard(boardPanel);
        add(boardPanel, BorderLayout.CENTER);

        updateTurnLabel();
        setVisible(true);
    }

    private void buildBoard(JPanel boardPanel) {
        for (int r=0;r<rows;r++) {
            for (int c=0;c<cols;c++) {
                JButton btn = createRoundedButton("");
                final int row=r,col=c;
                btn.addActionListener(e -> handleCellClick(row,col));
                cells[r][c]=btn;
                boardPanel.add(btn);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (gameOver || !isCellEmpty(row,col)) return;

        if (aiMode && currentTurn != Turn.PLAYER) return;
        if (!aiMode && currentTurn != Turn.LOCAL) return;

        placeMove(row,col,mySymbol,new Color(0,191,255));

        if(client != null && !aiMode){
            int moveIndex = row*cols + col;
            client.sendMove(moveIndex);
            currentTurn = Turn.REMOTE;
        } else if(aiMode){
            currentTurn = Turn.OPPONENT;
        }

        updateTurnLabel();
        checkGameOver(mySymbol);

        if(!gameOver && aiMode && currentTurn==Turn.OPPONENT){
            SwingUtilities.invokeLater(this::aiMove);
        }
    }

    private void aiMove() {
        if (gameOver || aiPlayer == null) return;

        int[] move = aiPlayer.chooseMove(this);
        if (move == null) return;

        int r = move[0], c = move[1];
        placeMove(r, c, opponentSymbol, new Color(255,69,0));
        checkGameOver(opponentSymbol);

        if(!gameOver){
            currentTurn = Turn.PLAYER;
            updateTurnLabel();
        }
    }

    private void placeMove(int row,int col,String symbol,Color color){
        cells[row][col].setText(symbol);
        cells[row][col].setForeground(color);
        setCell(row,col,symbol);
    }

    public void updateBoardFromServer(String message){
        if(!message.contains("MOVE")) return;

        int moveIndex = parseMoveIndex(message);
        int row = moveIndex / cols;
        int col = moveIndex % cols;
        if(!isCellEmpty(row,col)) return;

        placeMove(row,col,opponentSymbol,new Color(255,69,0));

        if(!aiMode) currentTurn = Turn.LOCAL;
        else currentTurn = Turn.PLAYER;

        updateTurnLabel();
        checkGameOver(opponentSymbol);
    }

    private int parseMoveIndex(String message) {
        int idx = message.indexOf("MOVE:");
        int start = message.indexOf("\"", idx)+1;
        int end = message.indexOf("\"", start);
        return Integer.parseInt(message.substring(start,end).trim());
    }

    private void checkGameOver(String symbol) {
        int[][] winCombos = {
                {0,0,0,1,0,2},{1,0,1,1,1,2},{2,0,2,1,2,2},
                {0,0,1,0,2,0},{0,1,1,1,2,1},{0,2,1,2,2,2},
                {0,0,1,1,2,2},{0,2,1,1,2,0}
        };

        for (int[] c:winCombos) {
            if (cells[c[0]][c[1]].getText().equals(symbol) &&
                    cells[c[2]][c[3]].getText().equals(symbol) &&
                    cells[c[4]][c[5]].getText().equals(symbol)) {
                handleGameOver(symbol.equals(mySymbol) ? "You win!" : "You lose!");
                return;
            }
        }

        if (isFull()) handleGameOver("Draw!");
    }

    private void handleGameOver(String message) {
        if (gameOver) return;
        gameOver=true;
        JOptionPane.showMessageDialog(this,message,"Game Over",JOptionPane.INFORMATION_MESSAGE);
        closeCallback.run();
    }

    private void updateTurnLabel() {
        if (gameOver) turnLabel.setText("Game over");
        else if (currentTurn==Turn.PLAYER || currentTurn==Turn.LOCAL) turnLabel.setText("Your turn: "+mySymbol);
        else turnLabel.setText("Opponent's turn: "+opponentSymbol);
    }

    private JButton createRoundedButton(String text) {
        Color buttonColor = new Color(44,44,46);
        JButton button = new JButton(text){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()?buttonColor.darker():buttonColor);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),30,30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFont(new Font("Arial",Font.BOLD,60));
        button.setForeground(Color.WHITE);
        return button;
    }

    public void setCloseCallback(Runnable callback){ this.closeCallback = callback!=null?callback:()->{}; }
}
