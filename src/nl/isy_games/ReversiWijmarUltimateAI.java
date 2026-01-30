package nl.isy_games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

public class ReversiWijmarUltimateAI extends ReversiSearchAI {

    private final double timeLimitSeconds;
    private final boolean aiIsX;

    // Use all available CPU cores
    private static final int PARALLELISM =
            Math.max(1, Runtime.getRuntime().availableProcessors());

    // Parallel search pool
    private static final ForkJoinPool SEARCH_POOL = new ForkJoinPool(
            PARALLELISM,
            pool -> {
                ForkJoinWorkerThread t =
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                t.setDaemon(true);
                t.setName("reversi-search-" + t.getPoolIndex());
                return t;
            },
            null,
            false
    );

    // Stop a bit before the real limit so we can collect results, request a stop for all threads & return a move within the time limit
    private static final long SAFETY_MARGIN_NANOS = 20_000_000L; // 20ms

    // Time check interval, every 1024 node visits
    private static final int TIME_CHECK_MASK = 0x3FF;

    // Below this depth, run sequentially to avoid fork/join overhead
    private static final int PARALLEL_DEPTH_THRESHOLD = 3;

    // Masks for board edges and corners
    private static final long NOT_A_FILE = 0xfefefefefefefefeL;
    private static final long NOT_H_FILE = 0x7f7f7f7f7f7f7f7fL;
    private static final long CORNERS =
            (1L << 0) | (1L << 7) | (1L << 56) | (1L << 63);

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

    // Maximum steps to expand a ray after the first shift
    private static final int MAX_RAY_SWEEPS = 6;

    private static final LongUnaryOperator[] DIRECTIONS = new LongUnaryOperator[]{
            ReversiWijmarUltimateAI::shiftEast,
            ReversiWijmarUltimateAI::shiftWest,
            ReversiWijmarUltimateAI::shiftNorth,
            ReversiWijmarUltimateAI::shiftSouth,
            ReversiWijmarUltimateAI::shiftNE,
            ReversiWijmarUltimateAI::shiftNW,
            ReversiWijmarUltimateAI::shiftSE,
            ReversiWijmarUltimateAI::shiftSW
    };

    private static final ThreadLocal<int[]> NODE_COUNTER =
            ThreadLocal.withInitial(() -> new int[1]);

    public ReversiWijmarUltimateAI(String name, String symbol, double timeLimitSeconds) {
        super(name, symbol);
        this.timeLimitSeconds = timeLimitSeconds > 0 ? timeLimitSeconds : 9.0;
        this.aiIsX = "X".equals(this.aiSymbol);
    }

    @Override
    public int[] chooseMove(ReversiGame board) {
        final FastBoard state = FastBoard.from(board.getBoardState());
        final long aiBits = aiIsX ? state.x : state.o;
        final long oppBits = aiIsX ? state.o : state.x;
        if (legalMoves(aiBits, oppBits) == 0L) return null;

        final long start = System.nanoTime();
        final long hardDeadline = start + (long) (timeLimitSeconds * 1_000_000_000L);
        final long softDeadline = Math.max(start, hardDeadline - SAFETY_MARGIN_NANOS);

        int bestMove = -1;
        int lastCompletedDepth = 0;

        int pvMove = -1; // best move from previous completed depth for root ordering

        for (int depth = 1; depth <= 64; depth++) {
            if (System.nanoTime() >= softDeadline) break;

            TimedResult r = bestMoveTimedParallel(state, depth, softDeadline, hardDeadline, pvMove);
            if (r.completed && r.move >= 0) {
                bestMove = r.move;
                lastCompletedDepth = depth;
                pvMove = bestMove;
            } else {
                break;
            }
        }

        if (bestMove < 0) {
            // Edge case: super mega ultra small time limit to guarantee a legal move
            bestMove = bestMoveDepth1Fallback(state);
        }

        // Print only when the deadline is hit
        if (System.nanoTime() >= softDeadline) {
            System.out.println("Timed AI max completed depth: " + lastCompletedDepth);
        }

        return bestMove >= 0 ? unpackMove(bestMove) : null;
    }

    // Another concurrency bullshit, dont even ask me how i got this working
    private TimedResult bestMoveTimedParallel(FastBoard board,
                                              int depth,
                                              long softDeadlineNanos,
                                              long hardDeadlineNanos,
                                              int pvMove) {

        final long aiBits = aiIsX ? board.x : board.o;
        final long oppBits = aiIsX ? board.o : board.x;
        final long movesMask = legalMoves(aiBits, oppBits);
        if (movesMask == 0L) return new TimedResult(-1, true);
        if (System.nanoTime() >= softDeadlineNanos) return new TimedResult(-1, false);

        final int moveCount = Long.bitCount(movesMask);
        final int[] ordered = new int[moveCount];
        int oi = 0;
        if (pvMove >= 0 && ((movesMask & (1L << pvMove)) != 0L)) {
            ordered[oi++] = pvMove;
        }
        for (int idx : MOVE_ORDER) {
            if (idx == pvMove) continue;
            if ((movesMask & (1L << idx)) != 0L) {
                ordered[oi++] = idx;
            }
        }

        // bestAtRoot packs score in the high 32 bits and move in the low 32 bits
        AtomicLong bestAtRoot = new AtomicLong(packBest(Integer.MIN_VALUE, -1));
        SearchContext ctx = new SearchContext(softDeadlineNanos, bestAtRoot);

        int firstMove = ordered[0];
        MoveScore first = scoreRootMove(board, firstMove, depth, ctx);
        if (!first.completed) {
            // Incompleted depth -> don't commit this iteration
            return new TimedResult(-1, false);
        }
        bestAtRoot.set(packBest(first.score, first.move));

        if (ordered.length == 1) {
            return new TimedResult(first.move, true);
        }

        // --- Parallelize remaining root moves ---
        ExecutorCompletionService<MoveScore> ecs = new ExecutorCompletionService<>(SEARCH_POOL);
        List<Future<MoveScore>> futures = new ArrayList<>(ordered.length - 1);

        for (int i = 1; i < ordered.length; i++) {
            final int mv = ordered[i];
            futures.add(ecs.submit(() -> scoreRootMove(board, mv, depth, ctx)));
        }

        boolean completed = true;
        int remaining = futures.size();

        while (remaining > 0) {
            long now = System.nanoTime();
            long wait = ctx.deadlineNanos - now;
            if (wait <= 0) {
                completed = false;
                break;
            }

            Future<MoveScore> f;
            try {
                f = ecs.poll(wait, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                completed = false;
                break;
            }

            if (f == null) {
                completed = false;
                break;
            }

            MoveScore ms;
            try {
                ms = f.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                completed = false;
                break;
            } catch (ExecutionException ee) {
                // Treat as incomplete depth & keep engine alive
                completed = false;
                ms = null;
            }

            remaining--;

            if (ms == null || !ms.completed) {
                completed = false;
                continue;
            }

            updateBestAtRoot(bestAtRoot, ms.score, ms.move);
        }

        if (!completed) {
            // Mutual stop for running tasks
            ctx.stop = true;

            // Cancel unstarted tasks
            for (Future<?> f : futures) f.cancel(false);

            // Use safety margin to "join" leftover tasks so they don't keep burning CPU
            // into the next turn, we only wait up to the hard deadline
            for (Future<?> f : futures) {
                if (f.isDone()) continue;
                long remain = hardDeadlineNanos - System.nanoTime();
                if (remain <= 0) break;
                try {
                    f.get(remain, TimeUnit.NANOSECONDS);
                } catch (TimeoutException ignored) {
                    break;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException ignored) {
                    // ignore
                }
            }

            return new TimedResult(-1, false);
        }

        // Completed depth -> return the best move
        long bestPacked = bestAtRoot.get();
        int bestMove = (int) bestPacked;
        return new TimedResult(bestMove, true);
    }

    private MoveScore scoreRootMove(FastBoard board, int move, int depth, SearchContext ctx) {
        if (ctx.stop) return MoveScore.incomplete(move);

        FastBoard next = applyMove(board, move, aiIsX);

        if (depth <= 1) {
            int score = evaluate(next);
            return new MoveScore(score, move, true);
        }

        // Initialize alpha with the current best root score to improve cross-thread pruning
        int alpha = Math.max(Integer.MIN_VALUE / 2, ctx.rootAlpha());
        int beta = Integer.MAX_VALUE / 2;

        // Use parallel minimax at all depths
        MinimaxTask task = new MinimaxTask(this, next, false, depth - 1, alpha, beta, ctx);
        int score = task.compute();

        // Depth is incomplete if stop was requested or time is up
        boolean completed = !ctx.stop && System.nanoTime() < ctx.deadlineNanos;
        return new MoveScore(score, move, completed);
    }

    // Sequential minimax with mutual stop and periodic time checks
    private int minimaxSequential(FastBoard board,
                                 boolean aiTurn,
                                 int depth,
                                 int alpha,
                                 int beta,
                                 SearchContext ctx) {

        if (depth <= 0) return evaluate(board);
        if (shouldStop(ctx)) return evaluate(board);

        boolean playerIsX = (aiIsX == aiTurn);
        long playerBits = playerIsX ? board.x : board.o;
        long oppBits = playerIsX ? board.o : board.x;

        long movesMask = legalMoves(playerBits, oppBits);
        if (movesMask == 0L) {
            long oppMoves = legalMoves(oppBits, playerBits);
            if (oppMoves == 0L) return terminalScore(board);
            return minimaxSequential(board, !aiTurn, depth - 1, alpha, beta, ctx);
        }

        boolean maximizing = aiTurn;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int move : MOVE_ORDER) {
            if ((movesMask & (1L << move)) == 0L) continue;

            FastBoard next = applyMove(board, move, playerIsX);
            int score = minimaxSequential(next, !aiTurn, depth - 1, alpha, beta, ctx);

            if (maximizing) {
                if (score > best) best = score;
                if (best > alpha) alpha = best;
                if (alpha >= beta) break;
            } else {
                if (score < best) best = score;
                if (best < beta) beta = best;
                if (alpha >= beta) break;
            }

            if (ctx.stop) break;
        }

        return best;
    }

    // Parallel minimax
    private static final class MinimaxTask extends RecursiveTask<Integer> {
        private final ReversiWijmarUltimateAI ai;
        private final FastBoard board;
        private final boolean aiTurn;
        private final int depth;
        private final int alpha;
        private final int beta;
        private final SearchContext ctx;

        MinimaxTask(ReversiWijmarUltimateAI ai, FastBoard board, boolean aiTurn,
                    int depth, int alpha, int beta, SearchContext ctx) {
            this.ai = ai;
            this.board = board;
            this.aiTurn = aiTurn;
            this.depth = depth;
            this.alpha = alpha;
            this.beta = beta;
            this.ctx = ctx;
        }

        @Override
        protected Integer compute() {
            if (depth <= 0) return ai.evaluate(board);
            if (shouldStop(ctx)) return ai.evaluate(board);

            boolean playerIsX = (ai.aiIsX == aiTurn);
            long playerBits = playerIsX ? board.x : board.o;
            long oppBits = playerIsX ? board.o : board.x;

            long movesMask = legalMoves(playerBits, oppBits);
            if (movesMask == 0L) {
                long oppMoves = legalMoves(oppBits, playerBits);
                if (oppMoves == 0L) return ai.terminalScore(board);
                // Pass turn
                return new MinimaxTask(ai, board, !aiTurn, depth - 1, alpha, beta, ctx).compute();
            }

            // Avoid fork overhead below depth threshold by using sequential search
            if (depth <= PARALLEL_DEPTH_THRESHOLD) {
                return ai.minimaxSequential(board, aiTurn, depth, alpha, beta, ctx);
            }

            boolean maximizing = aiTurn;

            int firstMove = firstMove(movesMask);
            FastBoard firstNext = applyMove(board, firstMove, playerIsX);
            int best = new MinimaxTask(ai, firstNext, !aiTurn, depth - 1, alpha, beta, ctx).compute();

            int localAlpha = alpha;
            int localBeta = beta;

            if (maximizing) {
                if (best > localAlpha) localAlpha = best;
                if (localAlpha >= localBeta) return best; // Cutoff
            } else {
                if (best < localBeta) localBeta = best;
                if (localAlpha >= localBeta) return best; // Cutoff
            }

            if (ctx.stop) {
                return best;
            }

            int remaining = Long.bitCount(movesMask) - 1;
            if (remaining <= 0) return best;

            MinimaxTask[] tasks = new MinimaxTask[remaining];
            int ti = 0;
            for (int move : MOVE_ORDER) {
                if (move == firstMove) continue;
                if ((movesMask & (1L << move)) == 0L) continue;
                FastBoard next = applyMove(board, move, playerIsX);
                MinimaxTask task = new MinimaxTask(ai, next, !aiTurn, depth - 1, localAlpha, localBeta, ctx);
                task.fork();
                tasks[ti++] = task;
            }

            // Join all forked tasks and update best
            for (int i = 0; i < ti; i++) {
                if (ctx.stop) {
                    // Cancel remaining tasks
                    for (int j = i; j < ti; j++) tasks[j].cancel(false);
                    break;
                }

                int score = tasks[i].join();

                if (maximizing) {
                    if (score > best) best = score;
                    if (best > localAlpha) localAlpha = best;
                    if (localAlpha >= localBeta) {
                        // Cutoff, cancel remaining tasks
                        for (int j = i + 1; j < ti; j++) tasks[j].cancel(false);
                        break;
                    }
                } else {
                    if (score < best) best = score;
                    if (best < localBeta) localBeta = best;
                    if (localAlpha >= localBeta) {
                        // Cutoff, cancel remaining tasks
                        for (int j = i + 1; j < ti; j++) tasks[j].cancel(false);
                        break;
                    }
                }
            }

            return best;
        }
    }

    // ----------------- Small utilities / bookkeeping -----------------

    private int bestMoveDepth1Fallback(FastBoard board) {
        long aiBits = aiIsX ? board.x : board.o;
        long oppBits = aiIsX ? board.o : board.x;
        long movesMask = legalMoves(aiBits, oppBits);
        if (movesMask == 0L) return -1;

        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        // Emergency fallback
        for (int move : MOVE_ORDER) {
            if ((movesMask & (1L << move)) == 0L) continue;
            FastBoard next = applyMove(board, move, aiIsX);
            int score = evaluate(next);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private static void updateBestAtRoot(AtomicLong bestAtRoot, int score, int move) {
        while (true) {
            long prev = bestAtRoot.get();
            int prevScore = (int) (prev >> 32);
            if (score <= prevScore) return;
            long next = packBest(score, move);
            if (bestAtRoot.compareAndSet(prev, next)) return;
        }
    }

    private static int firstMove(long movesMask) {
        for (int idx : MOVE_ORDER) {
            if ((movesMask & (1L << idx)) != 0L) return idx;
        }
        return -1;
    }

    private static int[] unpackMove(int move) {
        return new int[]{move >> 3, move & 7};
    }

    private static long packBest(int score, int move) {
        return (((long) score) << 32) | (move & 0xffffffffL);
    }

    private static boolean shouldStop(SearchContext ctx) {
        if (ctx.stop) return true;
        int[] counter = NODE_COUNTER.get();
        if ((++counter[0] & TIME_CHECK_MASK) == 0) {
            if (System.nanoTime() >= ctx.deadlineNanos) {
                ctx.stop = true;
                return true;
            }
        }
        return false;
    }

    private int evaluate(FastBoard board) {
        long aiBits = aiIsX ? board.x : board.o;
        long oppBits = aiIsX ? board.o : board.x;

        int score = scorePositions(aiBits) - scorePositions(oppBits);

        int mobility = Long.bitCount(legalMoves(aiBits, oppBits))
                - Long.bitCount(legalMoves(oppBits, aiBits));
        int pieceDiff = Long.bitCount(aiBits) - Long.bitCount(oppBits);
        int cornerDiff = Long.bitCount(aiBits & CORNERS) - Long.bitCount(oppBits & CORNERS);

        score += mobility * 4;
        score += pieceDiff;
        score += cornerDiff * 25;

        return score;
    }

    private static int scorePositions(long bits) {
        int score = 0;
        long remaining = bits;
        while (remaining != 0L) {
            int idx = Long.numberOfTrailingZeros(remaining);
            score += POSITION_WEIGHTS[idx];
            remaining &= remaining - 1;
        }
        return score;
    }

    private int terminalScore(FastBoard board) {
        int diff = Long.bitCount(board.x) - Long.bitCount(board.o);
        if (diff == 0) return 0;
        if (aiIsX) {
            return diff > 0 ? TERMINAL_SCORE + diff : -TERMINAL_SCORE + diff;
        }
        return diff < 0 ? TERMINAL_SCORE - diff : -TERMINAL_SCORE - diff;
    }

    private static long legalMoves(long me, long opp) {
        long empty = ~(me | opp);
        long moves = 0L;
        for (LongUnaryOperator shift : DIRECTIONS) {
            moves |= legalMovesInDirection(me, opp, empty, shift);
        }
        return moves;
    }

    private static long legalMovesInDirection(long me, long opp, long empty, LongUnaryOperator shift) {
        long m = opp & shift.applyAsLong(me);
        for (int i = 0; i < MAX_RAY_SWEEPS; i++) {
            m |= opp & shift.applyAsLong(m);
        }
        return empty & shift.applyAsLong(m);
    }

    private static long flips(long moveBit, long me, long opp) {
        long flips = 0L;
        for (LongUnaryOperator shift : DIRECTIONS) {
            flips |= flipsInDirection(moveBit, me, opp, shift);
        }
        return flips;
    }

    private static long flipsInDirection(long moveBit, long me, long opp, LongUnaryOperator shift) {
        long m = opp & shift.applyAsLong(moveBit);
        for (int i = 0; i < MAX_RAY_SWEEPS; i++) {
            m |= opp & shift.applyAsLong(m);
        }
        return (shift.applyAsLong(m) & me) != 0L ? m : 0L;
    }

    private static FastBoard applyMove(FastBoard board, int move, boolean playerIsX) {
        long moveBit = 1L << move;
        long me = playerIsX ? board.x : board.o;
        long opp = playerIsX ? board.o : board.x;

        long flipBits = flips(moveBit, me, opp);
        long newMe = me | moveBit | flipBits;
        long newOpp = opp & ~flipBits;

        return playerIsX ? new FastBoard(newMe, newOpp) : new FastBoard(newOpp, newMe);
    }

    private static long shiftEast(long b) {
        return (b & NOT_H_FILE) << 1;
    }

    private static long shiftWest(long b) {
        return (b & NOT_A_FILE) >>> 1;
    }

    private static long shiftNorth(long b) {
        return b >>> 8;
    }

    private static long shiftSouth(long b) {
        return b << 8;
    }

    private static long shiftNE(long b) {
        return (b & NOT_H_FILE) >>> 7;
    }

    private static long shiftNW(long b) {
        return (b & NOT_A_FILE) >>> 9;
    }

    private static long shiftSE(long b) {
        return (b & NOT_H_FILE) << 9;
    }

    private static long shiftSW(long b) {
        return (b & NOT_A_FILE) << 7;
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

    private static final class SearchContext {
        final long deadlineNanos;
        final AtomicLong bestAtRoot;
        volatile boolean stop;

        SearchContext(long deadlineNanos, AtomicLong bestAtRoot) {
            this.deadlineNanos = deadlineNanos;
            this.bestAtRoot = bestAtRoot;
        }

        int rootAlpha() {
            return (int) (bestAtRoot.get() >> 32);
        }
    }

    private static final class MoveScore {
        final int score;
        final int move;
        final boolean completed;

        MoveScore(int score, int move, boolean completed) {
            this.score = score;
            this.move = move;
            this.completed = completed;
        }

        static MoveScore incomplete(int move) {
            return new MoveScore(Integer.MIN_VALUE, move, false);
        }
    }

    private static final class TimedResult {
        final int move;
        final boolean completed;

        TimedResult(int move, boolean completed) {
            this.move = move;
            this.completed = completed;
        }
    }

    private static final class FastBoard {
        final long x;
        final long o;

        FastBoard(long x, long o) {
            this.x = x;
            this.o = o;
        }

        static FastBoard from(String[][] board) {
            long x = 0L;
            long o = 0L;
            int idx = 0;
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    String cell = board[r][c];
                    if ("X".equals(cell)) {
                        x |= 1L << idx;
                    } else if ("O".equals(cell)) {
                        o |= 1L << idx;
                    }
                    idx++;
                }
            }
            return new FastBoard(x, o);
        }
    }
}
