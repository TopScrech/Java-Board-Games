package nl.isy_games;

import java.util.ArrayList;

public class ReversiTimedAI extends ReversiSearchAI {

    private final double timeLimitSeconds;

    public ReversiTimedAI(String name, String symbol, double timeLimitSeconds) {
        super(name, symbol);
        this.timeLimitSeconds = timeLimitSeconds > 0 ? timeLimitSeconds : 9.0;
    }

    @Override
    public int[] chooseMove(ReversiGame board) {
        String[][] state = board.getBoardState();
        ArrayList<int[]> moves = legalMoves(state, aiSymbol);
        if (moves.isEmpty()) return null;

        long deadline = System.nanoTime() + (long) (timeLimitSeconds * 1_000_000_000L);
        int[] bestMove = null;
        int depth = 1;
        int maxDepth = 64;
        int lastCompletedDepth = 0;
        boolean timedOut = false;

        while (depth <= maxDepth && !isExpired(deadline)) {
            TimedResult result = bestMoveTimed(state, depth, deadline);
            if (result.completed && result.move != null) {
                bestMove = result.move;
                lastCompletedDepth = depth;
                depth++;
            } else {
                timedOut = isExpired(deadline);
                break;
            }
        }

        if (timedOut) {
            System.out.println("Timed AI max completed depth: " + lastCompletedDepth);
        }

        if (bestMove == null) {
            bestMove = bestMoveAtDepth(state, 1).move;
        }

        return bestMove;
    }

    private TimedResult bestMoveTimed(String[][] board, int depth, long deadline) {
        ArrayList<int[]> moves = legalMoves(board, aiSymbol);
        if (moves.isEmpty()) return new TimedResult(null, true);
        if (isExpired(deadline)) return new TimedResult(null, false);

        ArrayList<int[]> orderedMoves = orderedMoves(moves);
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        boolean[] timedOut = new boolean[]{false};

        for (int[] move : orderedMoves) {
            if (isExpired(deadline)) {
                timedOut[0] = true;
                break;
            }

            String[][] next = applyMove(board, move[0], move[1], aiSymbol);
            int score = depth <= 1
                    ? evaluate(next)
                    : minimaxTimed(next, opponentSymbol, depth - 1, alpha, beta, deadline, timedOut);

            if (timedOut[0]) break;

            if (score > bestScore) {
                bestScore = score;
                bestMove = new int[]{move[0], move[1]};
            }

            alpha = Math.max(alpha, bestScore);
        }

        return new TimedResult(bestMove, !timedOut[0]);
    }

    private TimedResult bestMoveAtDepth(String[][] board, int depth) {
        ArrayList<int[]> moves = legalMoves(board, aiSymbol);
        if (moves.isEmpty()) return new TimedResult(null, true);

        ArrayList<int[]> orderedMoves = orderedMoves(moves);
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        for (int[] move : orderedMoves) {
            String[][] next = applyMove(board, move[0], move[1], aiSymbol);
            int score = depth <= 1
                    ? evaluate(next)
                    : minimax(next, opponentSymbol, depth - 1, alpha, beta);

            if (score > bestScore) {
                bestScore = score;
                bestMove = new int[]{move[0], move[1]};
            }

            alpha = Math.max(alpha, bestScore);
        }

        return new TimedResult(bestMove, true);
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

    private int minimaxTimed(String[][] board, String player, int depth, int alpha, int beta,
                             long deadline, boolean[] timedOut) {
        if (timedOut[0] || isExpired(deadline)) {
            timedOut[0] = true;
            return evaluate(board);
        }

        if (depth <= 0) return evaluate(board);

        ArrayList<int[]> moves = legalMoves(board, player);
        if (moves.isEmpty()) {
            ArrayList<int[]> oppMoves = legalMoves(board, toggleSymbol(player));
            if (oppMoves.isEmpty()) return terminalScore(board);
            return minimaxTimed(board, toggleSymbol(player), depth - 1, alpha, beta, deadline, timedOut);
        }

        boolean maximizing = player.equals(aiSymbol);
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int[] move : orderedMoves(moves)) {
            if (timedOut[0] || isExpired(deadline)) {
                timedOut[0] = true;
                return best;
            }

            String[][] next = applyMove(board, move[0], move[1], player);
            int score = minimaxTimed(next, toggleSymbol(player), depth - 1, alpha, beta, deadline, timedOut);

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

    private static boolean isExpired(long deadline) {
        return System.nanoTime() >= deadline;
    }

    private static class TimedResult {
        private final int[] move;
        private final boolean completed;

        private TimedResult(int[] move, boolean completed) {
            this.move = move;
            this.completed = completed;
        }
    }
}
