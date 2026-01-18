package nl.isy_games;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TicTacToeRandomAI implements TicTacToeAI {
    private final String name;
    private final String symbol;

    public TicTacToeRandomAI(String name, String symbol) {
        this.name = name;
        this.symbol = symbol.toUpperCase();
    }

    @Override
    public int[] chooseMove(TicTacToeGame board) {
        String[][] state = board.getBoardState();
        ArrayList<int[]> options = new ArrayList<>();

        for (int r = 0; r < state.length; r++) {
            for (int c = 0; c < state[r].length; c++) {
                if (state[r][c].isEmpty()) {
                    options.add(new int[]{r, c});
                }
            }
        }

        if (options.isEmpty()) return null;

        int idx = ThreadLocalRandom.current().nextInt(options.size());
        int[] move = options.get(idx);
        return new int[]{move[0], move[1]};
    }
}
