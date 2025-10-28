package classes;

import javax.swing.*;
import java.awt.*;

public class TicTacToeGame extends BoardGame {

    private final JButton[][] cells;
    private boolean aiMode = false;
    private boolean gameOver = false;
    private AI aiPlayer;
    private Runnable closeCallback = () -> {};

    private enum Turn { PLAYER, OPPONENT }
    private Turn currentTurn;
    private final Turn initialTurn;

    private final String mySymbol;
    private final String opponentSymbol;

    private final JLabel turnLabel;

    private final GameClient client;

    private final int cellSize = 165;

    public TicTacToeGame(String gameType) {
        this(null, gameType, "X", "O", true);
        setAIMode(true);
    }

    public TicTacToeGame(GameClient client, String gameType, String mySymbol, String opponentSymbol, boolean playerFirst) {
        this.client = client;
        this.mySymbol = mySymbol;
        this.opponentSymbol = opponentSymbol;
        this.currentTurn = playerFirst ? Turn.PLAYER : Turn.OPPONENT;
        this.initialTurn = this.currentTurn;

        initializeBoard(3,3); 
        cells = new JButton[rows][cols];

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

        aiPlayer = new AI("Computer", opponentSymbol);

        updateTurnLabel();
        setVisible(true);
    }

    private void buildBoard(JPanel boardPanel) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createRoundedButton("");
                final int row = r, col = c;
                btn.addActionListener(e -> handleCellClick(row, col));
                cells[r][c] = btn;
                boardPanel.add(btn);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (gameOver || !isCellEmpty(row,col) || currentTurn != Turn.PLAYER) return;

        cells[row][col].setText(mySymbol);
        cells[row][col].setForeground(new Color(0,191,255));
        setCell(row,col,mySymbol);

        if (client != null && !aiMode) {
            int moveIndex = row*cols + col;
            client.sendMove(moveIndex);
        }

        currentTurn = Turn.OPPONENT;
        updateTurnLabel();
        checkGameOver(mySymbol);

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

        int r = move[0], c = move[1];
        cells[r][c].setText(opponentSymbol);
        cells[r][c].setForeground(new Color(255,69,0));
        setCell(r,c,opponentSymbol);

        checkGameOver(opponentSymbol);
        currentTurn = Turn.PLAYER;
        updateTurnLabel();
    }

    private void checkGameOver(String symbol) {
        int[][] winCombos = {
                {0,0,0,1,0,2}, {1,0,1,1,1,2}, {2,0,2,1,2,2},
                {0,0,1,0,2,0}, {0,1,1,1,2,1}, {0,2,1,2,2,2},
                {0,0,1,1,2,2}, {0,2,1,1,2,0}
        };

        for (int[] combo : winCombos) {
            if (cells[combo[0]][combo[1]].getText().equals(symbol) &&
                    cells[combo[2]][combo[3]].getText().equals(symbol) &&
                    cells[combo[4]][combo[5]].getText().equals(symbol)) {
                handleGameOver("Player " + symbol + " wins!");
                return;
            }
        }

        if (isFull()) handleGameOver("Draw!");
    }

    private void handleGameOver(String message) {
        if (gameOver) return;
        gameOver = true;

        Object[] options = aiMode ? new Object[]{"Restart","Close"} : new Object[]{"Close"};
        int choice = JOptionPane.showOptionDialog(this, message, "Game Over",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (aiMode && choice == 0) restartGame();
        else closeCallback.run();
    }

    private void restartGame() {
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++) {
                cells[r][c].setText("");
                cells[r][c].setForeground(Color.WHITE);
                setCell(r,c,"");
            }

        gameOver = false;
        currentTurn = initialTurn;
        updateTurnLabel();

        if (aiMode && currentTurn == Turn.OPPONENT)
            SwingUtilities.invokeLater(this::aiMove);
    }

    private void updateTurnLabel() {
        if (gameOver) turnLabel.setText("Game over");
        else if (currentTurn == Turn.PLAYER) turnLabel.setText("Your turn: " + mySymbol);
        else turnLabel.setText("Opponent's turn: " + opponentSymbol);
    }

    private JButton createRoundedButton(String text) {
        Color buttonColor = new Color(44,44,46);
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        button.setFont(new Font("Arial", Font.BOLD, 60));
        button.setForeground(Color.WHITE);
        return button;
    }

    public void setAIMode(boolean aiMode) { this.aiMode = aiMode; }

    public void setCloseCallback(Runnable callback) { this.closeCallback = callback!=null?callback:()->{}; }

    public void updateBoardFromServer(String message) {
        if (!message.contains("MOVE")) return;

        int moveIndex = parseMoveIndex(message);
        int row = moveIndex / cols;
        int col = moveIndex % cols;

        if (!isCellEmpty(row,col)) return;

        cells[row][col].setText(opponentSymbol);
        cells[row][col].setForeground(new Color(255,69,0));
        setCell(row,col,opponentSymbol);

        checkGameOver(opponentSymbol);
        currentTurn = Turn.PLAYER;
        updateTurnLabel();
    }

    private int parseMoveIndex(String message) {
        int idx = message.indexOf("MOVE:");
        int start = message.indexOf("\"", idx)+1;
        int end = message.indexOf("\"", start);
        return Integer.parseInt(message.substring(start,end).trim());
    }
    @Override
    public boolean isCellEmpty(int row, int col) {
        String[][] state = getBoardState();
        return state[row][col].isEmpty();
    }

    @Override
    public void setCell(int row, int col, String value) {
    }

    @Override
    public String[][] getBoardState() {
        String[][] state = new String[rows][cols];
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                state[r][c] = cells[r][c].getText().trim();
        return state;
    }

    public boolean isFull() {
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                if (cells[r][c].getText().isEmpty()) return false;
        return true;
    }
}
