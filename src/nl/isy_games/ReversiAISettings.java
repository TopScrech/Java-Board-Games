package nl.isy_games;

public final class ReversiAISettings {

    public enum AIType { TIMED, FIXED }

    private static AIType aiType = AIType.TIMED;
    private static int fixedDepth = 4;
    private static double timeLimitSeconds = 9.0;

    private ReversiAISettings() {}

    public static AIType getAiType() {
        return aiType;
    }

    public static void setAiType(AIType type) {
        if (type == null) return;
        aiType = type;
    }

    public static int getFixedDepth() {
        return fixedDepth;
    }

    public static void setFixedDepth(int depth) {
        if (depth < 1) return;
        fixedDepth = depth;
    }

    public static double getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
}
