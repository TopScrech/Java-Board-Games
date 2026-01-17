package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ReversiGame extends BoardGame {

    public enum Turn { PLAYER, OPPONENT, LOCAL, REMOTE }

    private final JButton[][] cells;
    private final int cellSize = 80;
    private final GameClient client;
    private final boolean myTurnFirst;
    private final boolean aiOpponentMode;

    private Turn currentTurn;
    private final Turn initialTurn;

    private String mySymbol;
    private String opponentSymbol;
    private String piece;

    private ReversiRules rules = new ReversiRules();
    private ReversiAI aiPlayer;
    private boolean aiMovePending = false;
    private Runnable closeCallback = () -> {};
    private boolean gameOver = false;
    private JDialog gameOverDialog;

    public ReversiGame(GameClient client, boolean myTurnFirst) {
        this(client, myTurnFirst, false);
    }

    public ReversiGame(GameClient client, boolean myTurnFirst, boolean enableAI) {
        super(8, 8);
        this.client = client;
        this.myTurnFirst = myTurnFirst;
        this.aiOpponentMode = enableAI;

        this.mySymbol = myTurnFirst ? "X" : "O";
        this.opponentSymbol = myTurnFirst ? "O" : "X";

        this.currentTurn = client == null
                ? (myTurnFirst ? Turn.PLAYER : Turn.OPPONENT)
                : (myTurnFirst ? Turn.LOCAL : Turn.REMOTE);
        this.initialTurn = currentTurn;

        cells = new JButton[rows][cols];

        initializeAiPlayer();
        setPiece();
        buildBoard();
        setupInitialPieces();
        updateCellColors();
        setVisible(true);

        printFirstPlayer();

        if (aiOpponentMode && client == null && currentTurn == Turn.OPPONENT) {
            scheduleOpponentAIMove();
        }
    }

    // For offline testing
    public ReversiGame() {
        this(null, true, false);
    }

    private void initializeAiPlayer() {
        if (!aiOpponentMode) {
            aiPlayer = null;
            return;
        }

        aiPlayer = new ReversiAI("Bot", opponentSymbol);
    }

    private void printFirstPlayer() {
        System.out.println(currentTurn == Turn.LOCAL || currentTurn == Turn.PLAYER ? "Player goes first" : "Opponent goes first");
    }

    private void buildBoard() {
        setLayout(new GridLayout(rows, cols, 2, 2));
        setBackground(new Color(28,28,30));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int finalR = r;
                int finalC = c;
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setColor(getBackground());
                        g2.fillRect(0, 0, getWidth(), getHeight());

                        // Draw game pieces
                        String val = board[finalR][finalC];
                        if (!val.equals("")) {
                            int padding = Math.max(8, Math.min(getWidth(), getHeight()) / 8);
                            int diameter = Math.min(getWidth(), getHeight()) - padding * 2;
                            int x = (getWidth() - diameter) / 2;
                            int y = (getHeight() - diameter) / 2;
                            g2.setColor(val.equals("X") ? Color.BLACK : Color.WHITE);
                            g2.fillOval(x, y, diameter, diameter);
                        }

                        g2.dispose();
                    }
                };

                btn.setPreferredSize(new Dimension(cellSize, cellSize));
                btn.setFocusPainted(false);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(new Color(20,20,20)));

                final int rr = r;
                final int cc = c;
                btn.addActionListener(e -> handleCellClick(rr, cc));

                cells[r][c] = btn;
                add(btn);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        System.out.println("[DEBUG] Click at: " + row + "," + col + " | CurrentTurn: " + currentTurn);

        if (gameOver) return;

        if (currentTurn != Turn.LOCAL && currentTurn != Turn.PLAYER) {
            System.out.println("[DEBUG] Not your turn, ignoring click.");
            return;
        }

        ArrayList<int[]> legalMoves = getLegalMovesFor(mySymbol);
        boolean isLegal = legalMoves.stream().anyMatch(move -> move[0]==row && move[1]==col);

        if (!isLegal) {
            System.out.println("[DEBUG] Illegal move attempted at: " + row + "," + col);
            return;
        }

        System.out.println("[DEBUG] Applying move at: " + row + "," + col + " for " + mySymbol);
        applyMove(row, col, mySymbol);
        checkGameOver();
        if (gameOver) {
            updateCellColors();
            return;
        }

        if (client != null) {
            int index = row * 8 + col;
            client.sendMove(index);
        }

        // Alleen wisselen van beurt als zet succesvol is gedaan
        if (client != null) {
            currentTurn = Turn.REMOTE;
        } else if (aiOpponentMode) {
            currentTurn = Turn.OPPONENT;
        } else {
            currentTurn = Turn.REMOTE;
        }
        setPiece();
        updateCellColors();
        printFirstPlayer();

        if (aiOpponentMode && client == null && currentTurn == Turn.OPPONENT) {
            scheduleOpponentAIMove();
        }
    }

    private void setupInitialPieces() {
        setCell(3,3,"O"); setCell(3,4,"X");
        setCell(4,3,"X"); setCell(4,4,"O");
    }

    private void updateCellColors() {
        ArrayList<int[]> legalMoves = new ArrayList<>();
        if (currentTurn == Turn.LOCAL || currentTurn == Turn.PLAYER) {
            legalMoves = getLegalMovesFor(mySymbol);
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (!isCellEmpty(r, c)) {
                    cells[r][c].setBackground(new Color(34,139,34)); // board color
                    continue;
                }

                boolean isLegal = false;
                for (int[] move : legalMoves) {
                    if (move[0] == r && move[1] == c) {
                        isLegal = true;
                        break;
                    }
                }

                cells[r][c].setBackground(
                        isLegal && (currentTurn == Turn.LOCAL || currentTurn == Turn.PLAYER)
                                ? Color.GREEN
                                : Color.GRAY
                );
            }
        }
    }

    public void setPiece() {
        piece = (currentTurn == Turn.LOCAL || currentTurn == Turn.PLAYER) ? mySymbol : opponentSymbol;
    }

    public void updateBoardFromServer(String message) {
        System.out.println("[SERVER] Received: " + message + " | CurrentTurn before: " + currentTurn);

        if (message.contains("YOURTURN")) {
            currentTurn = Turn.LOCAL;
            setPiece();
            updateCellColors();
            System.out.println("[DEBUG] It's your turn now.");
            return;
        }

        if (!message.contains("MOVE")) return;

        int moveIndex = parseMoveIndex(message);
        if (moveIndex < 0) return;

        int row = moveIndex / 8;
        int col = moveIndex % 8;

        String player = parseValue(message, "PLAYER");

        // Skip applyMove als het onze eigen zet was
        if (player != null && player.equalsIgnoreCase(client.getPlayerName()) && !isCellEmpty(row, col)) {
            System.out.println("[DEBUG] Skipping server echo of our own move at: " + row + "," + col);
            return;
        }

        if (!isCellEmpty(row, col)) {
            System.out.println("[DEBUG] Server move attempted on non-empty cell: " + row + "," + col);
            return;
        }

        String symbol = (player == null || !player.equalsIgnoreCase(client.getPlayerName())) ? opponentSymbol : mySymbol;

        System.out.println("[DEBUG] Applying server move at: " + row + "," + col + " for " + symbol);
        applyMove(row, col, symbol);

        if (symbol.equals(opponentSymbol)) {
            currentTurn = Turn.LOCAL;
            setPiece();
        }

        updateCellColors();
        printFirstPlayer();
        System.out.println("[DEBUG] CurrentTurn after server update: " + currentTurn);
    }


    private int parseMoveIndex(String message) {
        try {
            int idx = message.indexOf("MOVE:");
            int start = message.indexOf("\"",idx)+1;
            int end = message.indexOf("\"",start);
            return Integer.parseInt(message.substring(start,end).trim());
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static String parseValue(String message, String key) {
        try {
            int idx = message.indexOf(key + ":");
            if (idx < 0) return null;
            int start = message.indexOf("\"", idx) + 1;
            int end = message.indexOf("\"", start);
            return message.substring(start,end);
        } catch(Exception e) { return null; }
    }

    /**
     * Apply move AND print state
     */
    public void applyMove(int row, int col, String symbol) {

        System.out.println("[DEBUG] applyMove(" + row + "," + col + ") symbol=" + symbol);

        setCell(row,col,symbol);

        ArrayList<int[]> flips = rules.checkAllDirections(board,row,col,symbol);
        for (int[] pos : flips) {
            System.out.println("   flipping → (" + pos[0] + "," + pos[1] + ")");
            setCell(pos[0],pos[1],symbol);
        }

        repaint();
    }

    @Override
    public boolean isCellEmpty(int row,int col) { return board[row][col].equals(""); }

    @Override
    public void setCell(int row,int col,String value) {
        board[row][col]=value;
        if(cells[row][col]!=null) cells[row][col].repaint();
    }

    @Override
    public String[][] getBoardState() { return board; }

    public void setTurn(Turn t) {
        this.currentTurn = t;
        setPiece();
        updateCellColors();

        if (aiOpponentMode && client == null && currentTurn == Turn.OPPONENT) {
            scheduleOpponentAIMove();
        }
    }

    public void setCloseCallback(Runnable callback){
        this.closeCallback = callback!=null ? callback : () -> {};
    }

    public ArrayList<int[]> getLegalMovesFor(String symbol) {
        return rules.getLegalMoves(board, symbol);
    }

    private boolean hasLegalMove(String symbol) {
        return !getLegalMovesFor(symbol).isEmpty();
    }

    private void scheduleOpponentAIMove() {
        if (!aiOpponentMode || client != null || aiMovePending) return;
        aiMovePending = true;
        SwingUtilities.invokeLater(() -> {
            aiMovePending = false;
            aiMove();
        });
    }

    private void aiMove() {
        if (!aiOpponentMode || client != null || aiPlayer == null) return;
        if (currentTurn != Turn.OPPONENT) return;
        if (gameOver) return;

        int[] move = aiPlayer.chooseMove(this);
        if (move == null) {
            handleNoLegalMovesForOpponent();
            return;
        }

        int row = move[0];
        int col = move[1];

        applyMove(row, col, opponentSymbol);
        checkGameOver();
        if (gameOver) {
            updateCellColors();
            return;
        }

        currentTurn = Turn.PLAYER;
        setPiece();
        updateCellColors();
        printFirstPlayer();

        handleNoLegalMovesForPlayer();
    }

    private void handleNoLegalMovesForOpponent() {
        if (currentTurn != Turn.OPPONENT) return;

        if (hasLegalMove(mySymbol)) {
            currentTurn = Turn.PLAYER;
            setPiece();
            updateCellColors();
            printFirstPlayer();
            return;
        }

        // No legal moves for either player; leave board as-is.
        currentTurn = Turn.PLAYER;
        setPiece();
        updateCellColors();
        checkGameOver();
    }

    private void handleNoLegalMovesForPlayer() {
        if (currentTurn != Turn.PLAYER) return;
        if (hasLegalMove(mySymbol)) return;
        if (!hasLegalMove(opponentSymbol)) {
            checkGameOver();
            return;
        }

        currentTurn = Turn.OPPONENT;
        setPiece();
        updateCellColors();
        printFirstPlayer();
        scheduleOpponentAIMove();
    }

    private void checkGameOver() {
        if (client != null || gameOver) return;

        boolean noMovesPlayer = !hasLegalMove(mySymbol);
        boolean noMovesOpponent = !hasLegalMove(opponentSymbol);

        if (!isFull() && !(noMovesPlayer && noMovesOpponent)) return;

        int myCount = countPieces(mySymbol);
        int opponentCount = countPieces(opponentSymbol);

        if (myCount > opponentCount) handleGameOver("You win!");
        else if (myCount < opponentCount) handleGameOver("You lose!");
        else handleGameOver("Draw!");
    }

    private int countPieces(String symbol) {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (symbol.equals(board[r][c])) count++;
            }
        }
        return count;
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

        if (gameOverDialog != null) return;

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        pane.setOptions(new Object[]{"OK"});
        JDialog dialog = pane.createDialog(this, "Game Over");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                gameOverDialog = null;
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                gameOverDialog = null;
            }
        });

        gameOverDialog = dialog;
        dialog.setVisible(true);
    }

}
