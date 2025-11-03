package nl.isy_games;

import java.util.concurrent.ThreadLocalRandom;

public class AI {
    private final String name;
    private final String symbol;
    private final String opponentSymbol;

    public AI(String name, String symbol) {
        this.name = name;
        this.symbol = symbol.toUpperCase();
        this.opponentSymbol = togglePlayer(this.symbol);
    }

    public int[] chooseMove(TicTacToeGame board) {
        String[][] state = board.getBoardState();

        if (isBoardEmpty(state)) {
            int row = ThreadLocalRandom.current().nextInt(state.length);
            int col = ThreadLocalRandom.current().nextInt(state[0].length);
            return new int[]{row, col};
        }

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        String bestFingerprint = null;

        for (int r = 0; r < state.length; r++) {
            for (int c = 0; c < state[r].length; c++) {
                if (!state[r][c].isEmpty()) continue;

                state[r][c] = symbol;
                int score = minimax(state, opponentSymbol, 1);
                String fingerprint = fingerprint(state);
                state[r][c] = "";

                if (score > bestScore || (score == bestScore && isFingerprintBetter(fingerprint, bestFingerprint))) {
                    bestScore = score;
                    bestMove = new int[]{r, c};
                    bestFingerprint = fingerprint;
                }
            }
        }

        return bestMove;
    }

    private int minimax(String[][] board, String playerToMove, int depth) {
        Integer terminal = terminalScore(board, depth);
        if (terminal != null) return terminal;

        boolean maximizing = playerToMove.equals(symbol);
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                if (!board[r][c].isEmpty()) continue;

                board[r][c] = playerToMove;
                int score = minimax(board, togglePlayer(playerToMove), depth + 1);
                board[r][c] = "";

                if (maximizing) {
                    if (score > best) best = score;
                } else {
                    if (score < best) best = score;
                }
            }
        }

        return best;
    }

    private Integer terminalScore(String[][] board, int depth) {
        int eval = evaluate(board);

        if (eval != 0 || isFull(board)) {
            return scoreFromEval(eval, depth);
        }

        return null;
    }

    private int scoreFromEval(int eval, int depth) {
        int perspective = symbol.equals("X") ? eval : -eval;

        if (perspective == 1) return 10 - depth;
        if (perspective == -1) return depth - 10;

        return 0;
    }

    private static boolean isBoardEmpty(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                if (!cell.isEmpty()) return false;
            }
        }

        return true;
    }

    private static boolean isFull(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) return false;
            }
        }

        return true;
    }

    private static String togglePlayer(String player) {
        return "X".equals(player) ? "O" : "X";
    }

    private int evaluate(String[][] board) {
        if (playerHasWin("X", board)) return 1;
        if (playerHasWin("O", board)) return -1;
        return 0;
    }

    private boolean playerHasWin(String player, String[][] board) {
        for (int r = 0; r < 3; r++) {
            if (board[r][0].equals(player) && board[r][1].equals(player) && board[r][2].equals(player)) return true;
        }

        for (int c = 0; c < 3; c++) {
            if (board[0][c].equals(player) && board[1][c].equals(player) && board[2][c].equals(player)) return true;
        }

        if (board[0][0].equals(player) && board[1][1].equals(player) && board[2][2].equals(player)) return true;

        return board[0][2].equals(player) && board[1][1].equals(player) && board[2][0].equals(player);
    }

    private static String fingerprint(String[][] board) {
        StringBuilder sb = new StringBuilder(board.length * board[0].length);

        for (String[] row : board) {
            for (String cell : row) {
                sb.append(cell.isEmpty() ? " " : cell);
            }
        }

        return sb.toString();
    }

    private static boolean isFingerprintBetter(String candidate, String currentBest) {
        if (currentBest == null) return true;
        return candidate.compareTo(currentBest) < 0;
    }
}
