package nl.isy_games;

public interface GameAI<T extends BoardGame> {
    int[] chooseMove(T board);
}
