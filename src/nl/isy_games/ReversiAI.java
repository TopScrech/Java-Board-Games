package nl.isy_games;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ReversiAI implements GameAI<ReversiGame> {
    private final String name;
    private final String symbol;

    public ReversiAI(String name, String symbol) {
        this.name = name;
        this.symbol = symbol.toUpperCase();
    }

    @Override
    public int[] chooseMove(ReversiGame board) {
        ArrayList<int[]> legalMoves = board.getLegalMovesFor(symbol);
        if (legalMoves.isEmpty()) return null;

        int idx = ThreadLocalRandom.current().nextInt(legalMoves.size());
        int[] move = legalMoves.get(idx);
        return new int[]{move[0], move[1]};
    }
}
