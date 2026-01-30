package nl.isy_games;

import java.util.ArrayList;
import java.util.Arrays;

abstract class ReversiSearchAI extends ReversiAI {

    protected static final int BOARD_SIZE = 8;
    protected static final int TERMINAL_SCORE = 1_000_000;

    protected final String aiSymbol;
    protected final String opponentSymbol;
    protected final ReversiRules rules = new ReversiRules();

    // Source: https://reversiworld.wordpress.com/category/weighted-square-value
    private static final int[] POSITION_WEIGHTS = {
            120, -20, 20, 5, 5, 20, -20, 120,
            -20, -40, -5, -5, -5, -5, -40, -20,
            20, -5, 15, 3, 3, 15, -5, 20,
            5, -5, 3, 3, 3, 3, -5, 5,
            5, -5, 3, 3, 3, 3, -5, 5,
            20, -5, 15, 3, 3, 15, -5, 20,
            -20, -40, -5, -5, -5, -5, -40, -20,
            120, -20, 20, 5, 5, 20, -20, 120
    };

    private static final int[] MOVE_ORDER = buildMoveOrder();

    protected ReversiSearchAI(String name, String symbol) {
        super(name, symbol);
        this.aiSymbol = symbol.toUpperCase();
        this.opponentSymbol = toggleSymbol(this.aiSymbol);
    }

    protected ArrayList<int[]> orderedMoves(ArrayList<int[]> moves) {
        boolean[] present = new boolean[BOARD_SIZE * BOARD_SIZE];
        for (int[] move : moves) {
            present[move[0] * BOARD_SIZE + move[1]] = true;
        }

        ArrayList<int[]> ordered = new ArrayList<>(moves.size());
        for (int idx : MOVE_ORDER) {
            if (present[idx]) {
                ordered.add(new int[]{idx / BOARD_SIZE, idx % BOARD_SIZE});
            }
        }

        return ordered;
    }

    protected ArrayList<int[]> legalMoves(String[][] board, String symbol) {
        return rules.getLegalMoves(board, symbol);
    }

    protected String[][] applyMove(String[][] board, int row, int col, String symbol) {
        String[][] next = copyBoard(board);
        next[row][col] = symbol;

        ArrayList<int[]> flips = rules.checkAllDirections(next, row, col, symbol);
        for (int[] pos : flips) {
            next[pos[0]][pos[1]] = symbol;
        }

        return next;
    }

    protected boolean isFull(String[][] board) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c].isEmpty()) return false;
            }
        }
        return true;
    }

    protected int terminalScore(String[][] board) {
        int diff = countPieces(board, "X") - countPieces(board, "O");
        if (diff == 0) return 0;

        if (aiSymbol.equals("X")) {
            return diff > 0 ? TERMINAL_SCORE + diff : -TERMINAL_SCORE + diff;
        }

        return diff < 0 ? TERMINAL_SCORE - diff : -TERMINAL_SCORE - diff;
    }

    protected int evaluate(String[][] board) {
        int score = 0;
        int mobility = orderedMoves(legalMoves(board, aiSymbol)).size()
                - orderedMoves(legalMoves(board, opponentSymbol)).size();
        int pieceDiff = countPieces(board, aiSymbol) - countPieces(board, opponentSymbol);
        int cornerDiff = countCorners(board, aiSymbol) - countCorners(board, opponentSymbol);

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                String cell = board[r][c];
                if (cell.isEmpty()) continue;

                int weight = POSITION_WEIGHTS[r * BOARD_SIZE + c];
                if (cell.equals(aiSymbol)) {
                    score += weight;
                } else if (cell.equals(opponentSymbol)) {
                    score -= weight;
                }
            }
        }

        score += mobility * 4;
        score += pieceDiff;
        score += cornerDiff * 25;

        return score;
    }

    protected int countPieces(String[][] board, String symbol) {
        int count = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (symbol.equals(board[r][c])) count++;
            }
        }
        return count;
    }

    protected int countCorners(String[][] board, String symbol) {
        int count = 0;
        if (symbol.equals(board[0][0])) count++;
        if (symbol.equals(board[0][BOARD_SIZE - 1])) count++;
        if (symbol.equals(board[BOARD_SIZE - 1][0])) count++;
        if (symbol.equals(board[BOARD_SIZE - 1][BOARD_SIZE - 1])) count++;
        return count;
    }

    protected static String toggleSymbol(String symbol) {
        return "X".equals(symbol) ? "O" : "X";
    }

    protected static String[][] copyBoard(String[][] board) {
        String[][] copy = new String[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            copy[r] = Arrays.copyOf(board[r], board[r].length);
        }
        return copy;
    }

    private static int[] buildMoveOrder() {
        Integer[] indices = new Integer[BOARD_SIZE * BOARD_SIZE];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, (a, b) -> Integer.compare(POSITION_WEIGHTS[b], POSITION_WEIGHTS[a]));
        int[] order = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            order[i] = indices[i];
        }
        return order;
    }
}
