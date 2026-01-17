package nl.isy_games;

import java.util.ArrayList;

public class ReversiFixedDepthAI extends ReversiSearchAI {

    private final int maxDepth;

    public ReversiFixedDepthAI(String name, String symbol, int depth) {
        super(name, symbol);
        this.maxDepth = Math.max(1, depth);
    }

    @Override
    public int[] chooseMove(ReversiGame board) {
        String[][] state = board.getBoardState();
        ArrayList<int[]> moves = legalMoves(state, aiSymbol);
        if (moves.isEmpty()) return null;

        ArrayList<int[]> orderedMoves = orderedMoves(moves);
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        for (int[] move : orderedMoves) {
            String[][] next = applyMove(state, move[0], move[1], aiSymbol);
            int score = maxDepth <= 1
                    ? evaluate(next)
                    : minimax(next, opponentSymbol, maxDepth - 1, alpha, beta);

            if (score > bestScore) {
                bestScore = score;
                bestMove = new int[]{move[0], move[1]};
            }

            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    private int minimax(String[][] board, String player, int depth, int alpha, int beta) {
        if (depth <= 0) return evaluate(board);

        ArrayList<int[]> moves = legalMoves(board, player);
        if (moves.isEmpty()) {
            ArrayList<int[]> oppMoves = legalMoves(board, toggleSymbol(player));
            if (oppMoves.isEmpty()) return terminalScore(board);
            return minimax(board, toggleSymbol(player), depth - 1, alpha, beta);
        }

        boolean maximizing = player.equals(aiSymbol);
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int[] move : orderedMoves(moves)) {
            String[][] next = applyMove(board, move[0], move[1], player);
            int score = minimax(next, toggleSymbol(player), depth - 1, alpha, beta);

            if (maximizing) {
                if (score > best) best = score;
                alpha = Math.max(alpha, best);
                if (alpha >= beta) break;
            } else {
                if (score < best) best = score;
                beta = Math.min(beta, best);
                if (alpha >= beta) break;
            }
        }

        return best;
    }
}
