package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TicTacToeGame extends BoardGame {

    public enum Turn { PLAYER, OPPONENT, LOCAL, REMOTE }

    private final JButton[][] cells;
    private final boolean aiOpponentMode;
    private final boolean aiControlsLocal;
    private boolean gameOver = false;
    private AI aiPlayer;
    private Runnable closeCallback = () -> {};
    private boolean localAiMovePending = false;
    private JDialog gameOverDialog;
    private Runnable gameOverDialogClosedListener = () -> {};

    private Turn currentTurn;
    private final Turn initialTurn;

    private String mySymbol;
    private String opponentSymbol;

    private JLabel turnLabel;

    private final GameClient client;
    private final int cellSize = 200;
    private static final int AI_MOVE_DELAY_MS = 1000;
    private static final int AI_RETRY_DELAY_MS = 100;

    public TicTacToeGame(GameClient client, boolean playerFirst, boolean enableAI) {
        this(client, playerFirst, enableAI, false);
    }

    public TicTacToeGame(GameClient client, boolean playerFirst, boolean enableAI, boolean aiControlsLocal) {
        super(3,3);
        this.client = client;
        this.mySymbol = "X";
        this.opponentSymbol = "O";
        if (client == null) {
            this.currentTurn = playerFirst ? Turn.PLAYER : Turn.OPPONENT;
        } else {
            this.currentTurn = playerFirst ? Turn.LOCAL : Turn.REMOTE;
        }
        this.initialTurn = this.currentTurn;
        this.cells = new JButton[rows][cols];
        this.aiOpponentMode = enableAI;
        this.aiControlsLocal = aiControlsLocal;

        initializeAiPlayer();

        initializeUI();

        if (aiOpponentMode && currentTurn == Turn.OPPONENT) {
            scheduleOpponentAIMove();
        } else if (aiControlsLocal) {
            triggerLocalAIMoveIfNeeded();
        }
    }

    private void initializeAiPlayer() {
        if (!(aiOpponentMode || aiControlsLocal)) {
            aiPlayer = null;
            return;
        }

        String symbolForAI = aiControlsLocal ? mySymbol : opponentSymbol;
        aiPlayer = new AI("Bot", symbolForAI);
    }

    public void setSymbols(String my, String opponent) {
        this.mySymbol = my;
        this.opponentSymbol = opponent;
        initializeAiPlayer();
        if (aiControlsLocal) triggerLocalAIMoveIfNeeded();
    }

    public void setTurn(Turn t) {
        this.currentTurn = t;
        updateTurnLabel();
    }

    public void triggerAutoMoveIfNeeded() {
        triggerLocalAIMoveIfNeeded();
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

        if (aiControlsLocal) return;
        if (aiOpponentMode && currentTurn != Turn.PLAYER) return;
        if (!aiOpponentMode && currentTurn != Turn.LOCAL) return;

        placeMove(row,col,mySymbol,new Color(0,191,255));

        if(client != null && !aiOpponentMode){
            int moveIndex = row*cols + col;
            client.sendMove(moveIndex);
            currentTurn = Turn.REMOTE;
        } else if(aiOpponentMode){
            currentTurn = Turn.OPPONENT;
        }

        updateTurnLabel();
        checkGameOver(mySymbol);

        if(!gameOver && aiOpponentMode && currentTurn==Turn.OPPONENT){
            scheduleOpponentAIMove();
        }
    }

    private void aiMove() {
        if (gameOver || aiPlayer == null) return;

        if (aiControlsLocal) {
            performLocalAIMove();
            return;
        }

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

    private void triggerLocalAIMoveIfNeeded() {
        if (!aiControlsLocal || gameOver || currentTurn != Turn.LOCAL || localAiMovePending) return;
        localAiMovePending = true;
        scheduleOnEDT(AI_MOVE_DELAY_MS, () -> {
            localAiMovePending = false;
            aiMove();
        });
    }

    private void performLocalAIMove() {
        if (gameOver) return;

        int[] move = aiPlayer.chooseMove(this);
        if (move == null) {
            scheduleLocalAiRetry();
            return;
        }

        int r = move[0], c = move[1];
        if (!isCellEmpty(r, c)) {
            scheduleLocalAiRetry();
            return;
        }

        placeMove(r, c, mySymbol, new Color(0,191,255));

        if (client != null) {
            int moveIndex = r * cols + c;
            client.sendMove(moveIndex);
        }

        checkGameOver(mySymbol);

        if (!gameOver) {
            currentTurn = Turn.REMOTE;
            updateTurnLabel();
        }
    }

    private void scheduleLocalAiRetry() {
        if (gameOver) return;
        if (localAiMovePending) return;
        localAiMovePending = true;
        scheduleOnEDT(AI_RETRY_DELAY_MS, () -> {
            localAiMovePending = false;
            aiMove();
        });
    }

    private void scheduleOpponentAIMove() {
        scheduleOnEDT(AI_MOVE_DELAY_MS, this::aiMove);
    }

    private void placeMove(int row,int col,String symbol,Color color){
        cells[row][col].setText(symbol);
        cells[row][col].setForeground(color);
        setCell(row,col,symbol);
    }

    public void updateBoardFromServer(String message){
        if (message.contains("YOURTURN")) {
            currentTurn = Turn.LOCAL;
            updateTurnLabel();
            triggerLocalAIMoveIfNeeded();
            return;
        }

        if(!message.contains("MOVE")) return;

        int moveIndex = parseMoveIndex(message);
        int row = moveIndex / cols;
        int col = moveIndex % cols;
        if(!isCellEmpty(row,col)) return;

        placeMove(row,col,opponentSymbol,new Color(255,69,0));

        if (aiControlsLocal) currentTurn = Turn.LOCAL;
        else if (aiOpponentMode) currentTurn = Turn.PLAYER;
        else currentTurn = Turn.LOCAL;

        updateTurnLabel();
        checkGameOver(opponentSymbol);
        triggerLocalAIMoveIfNeeded();
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
        gameOver = true;
        showGameOverDialog(message);
        closeCallback.run();
    }

    private void showGameOverDialog(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showGameOverDialog(message));
            return;
        }

        dismissGameOverDialog();

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        pane.setOptions(new Object[]{"OK"});
        JDialog dialog = pane.createDialog(this, "Game Over");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                gameOverDialog = null;
                notifyGameOverDialogClosed();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                gameOverDialog = null;
            }
        });

        gameOverDialog = dialog;
        dialog.setVisible(true);
    }

    public void dismissGameOverDialog() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::dismissGameOverDialog);
            return;
        }

        if (gameOverDialog == null) return;

        JDialog dialog = gameOverDialog;
        gameOverDialog = null;
        dialog.dispose();
    }

    public void setGameOverDialogClosedListener(Runnable listener) {
        gameOverDialogClosedListener = listener != null ? listener : () -> {};
    }

    private void notifyGameOverDialogClosed() {
        Runnable listener = gameOverDialogClosedListener;
        gameOverDialogClosedListener = () -> {};
        if (listener != null) {
            listener.run();
        }
    }

    public void resetBoardState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::resetBoardState);
            return;
        }

        dismissGameOverDialog();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c] = "";
                JButton cell = cells[r][c];
                if (cell != null) {
                    cell.setText("");
                    cell.setForeground(Color.WHITE);
                }
            }
        }

        gameOver = false;
        localAiMovePending = false;
        currentTurn = initialTurn;
        updateTurnLabel();
    }

    private void updateTurnLabel() {
        if (gameOver) {
            turnLabel.setText("Game over");
            return;
        }

        switch (currentTurn) {
            case PLAYER:
                turnLabel.setText("Your turn: " + mySymbol);
                break;
            case OPPONENT:
                turnLabel.setText("Opponent's turn: " + opponentSymbol);
                break;
            case LOCAL:
                if (aiControlsLocal) turnLabel.setText("AI playing as " + mySymbol);
                else turnLabel.setText("Your turn: " + mySymbol);
                break;
            case REMOTE:
            default:
                turnLabel.setText("Opponent's turn: " + opponentSymbol);
                break;
        }
    }

    private void scheduleOnEDT(int delayMs, Runnable action) {
        if (delayMs <= 0) {
            SwingUtilities.invokeLater(action);
            return;
        }

        Timer timer = new Timer(delayMs, e -> action.run());
        timer.setRepeats(false);
        timer.start();
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
