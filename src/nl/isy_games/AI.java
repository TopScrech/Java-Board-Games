package classes;

public class AI {
    private final String name;
    private final String symbol;

    public AI(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public int[] chooseMove(TicTacToeGame board) {
        // Pick first empty cell (simple AI)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board.isCellEmpty(r, c)) return new int[]{r, c};
            }
        }
        return null;
    }
}
