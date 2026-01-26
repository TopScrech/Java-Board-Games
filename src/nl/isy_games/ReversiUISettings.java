package nl.isy_games;

import java.awt.Color;
import java.util.EnumMap;

public class ReversiUISettings {
    private ReversiUISettings() {}

    public enum ColorRole {
        PLAYER_PIECE,
        OPPONENT_PIECE,
        LEGAL_MOVE,
        BUTTON
    }

    private static final EnumMap<ColorRole, Color> selectedColors =
            new EnumMap<>(ColorRole.class);

    public static final Color[] PIECE_COLORS = {
            Color.BLACK,
            Color.WHITE,
            new Color(0, 102, 204),
            new Color(200, 0, 0),
            new Color(255, 105, 180)
    };

    public static final Color[] UI_COLORS = {
            Color.DARK_GRAY,
            new Color(0, 160, 0),
            new Color(255, 140, 0),
            new Color(20, 40, 120),
            new Color(120, 40, 120)
    };

    private static final EnumMap<ColorRole, Color> colors =
            new EnumMap<>(ColorRole.class);

    static {
        colors.put(ColorRole.PLAYER_PIECE, Color.BLACK);
        colors.put(ColorRole.OPPONENT_PIECE, Color.WHITE);
        colors.put(ColorRole.LEGAL_MOVE, UI_COLORS[1]);
        colors.put(ColorRole.BUTTON, UI_COLORS[0]);
    }

    public static Color getColor(ColorRole role) {
        return colors.get(role);
    }

    public static void setColor(ColorRole role, Color color) {
        colors.put(role, color);
    }

    public static boolean isColorUsed(Color color, ColorRole except) {
        for (ColorRole r : colors.keySet()) {
            if (r != except && colors.get(r).equals(color)) {
                return true;
            }
        }
        return false;
    }
}

