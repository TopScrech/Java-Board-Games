package nl.isy_games;

public final class ReversiAISettings {

    public enum AIType { TIMED, FIXED }

    public static final int DEFAULT_FIXED_DEPTH = 7;
    public static final double DEFAULT_TIME_LIMIT_SECONDS = 9.0;

    private static AIType aiType = AIType.TIMED;
    private static int fixedDepth = DEFAULT_FIXED_DEPTH;
    private static double timeLimitSeconds = DEFAULT_TIME_LIMIT_SECONDS;

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

    public static ReversiAI createAi(String symbol) {
        if (aiType == AIType.FIXED) {
            return new ReversiFixedDepthAI("Bot", symbol, fixedDepth);
        }
        return new ReversiWijmarUltimateAI("Bot", symbol, timeLimitSeconds);
    }
}
