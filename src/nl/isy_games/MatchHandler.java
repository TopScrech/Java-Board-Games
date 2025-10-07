package classes;

import javax.swing.*;

public class MatchHandler {

    public static void handleIncomingChallenge(GameClient client) {
        client.addServerListener(message -> {
            if (!message.contains("SVR GAME CHALLENGE")) return;

            String challenger = parseValue(message, "CHALLENGER");
            int challengeNumber = Integer.parseInt(parseValue(message, "CHALLENGENUMBER"));
            String gameType = parseValue(message, "GAMETYPE");

            // Ignore challenges sent by yourself
            if (challenger.equalsIgnoreCase(client.getPlayerName())) return;

            SwingUtilities.invokeLater(() -> {
                int response = JOptionPane.showConfirmDialog(
                        null,
                        challenger + " has challenged you to " + gameType + ".\nDo you want to accept?",
                        "Incoming Challenge",
                        JOptionPane.YES_NO_OPTION
                );

                if (response == JOptionPane.YES_OPTION) {
                    client.acceptChallenge(challengeNumber);
                    System.out.println("DEBUG: Challenge accepted #" + challengeNumber);
                } else {
                    client.denyChallenge(challengeNumber);
                    System.out.println("DEBUG: Challenge denied #" + challengeNumber);
                }
            });
        });
    }

    private static String parseValue(String message, String key) {
        try {
            int idx = message.indexOf(key + ":");
            if (idx < 0) return null;
            int start = message.indexOf("\"", idx) + 1;
            int end = message.indexOf("\"", start);
            return message.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
