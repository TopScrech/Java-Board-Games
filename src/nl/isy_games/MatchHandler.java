package classes;

import javax.swing.*;
import java.util.List;
import java.io.IOException;

public class MatchHandler {

    public static void startRandomMatchmaking(GameClient client, String gameName, GameModeSelector selector) {
        new Thread(() -> {
            try {
                boolean opponentFound = false;
                final boolean[] boardShown = {false};

                System.out.println("DEBUG: Subscribing for Random PvP: " + gameName);
                client.subscribe(gameName);
                System.out.println("DEBUG: Subscribed to " + gameName);

                client.setServerListener(message -> {
                    System.out.println("DEBUG: Server message received: " + message);

                    if (message.contains("SVR GAME MATCH") && !boardShown[0]) {
                        boardShown[0] = true;
                        SwingUtilities.invokeLater(() -> {
                            TicTacToeGame board = new TicTacToeGame(client);
                            board.setVisible(true);

                            selector.setMatchInProgress(true);
                            selector.setStatusLabelText("Match found! Opponent: " + parseValue(message, "OPPONENT"));

                            System.out.println("DEBUG: Match created! Opponent: " + parseValue(message, "OPPONENT"));
                        });
                    }
                });

                while (!opponentFound) {
                    List<String> players = client.getPlayerList();
                    System.out.println("DEBUG: Current players for Random PvP: " + players);

                    for (String p : players) {
                        if (!p.equalsIgnoreCase(client.getPlayerName())) {
                            System.out.println("DEBUG: Found possible opponent: " + p);
                            client.challenge(p, gameName);
                            opponentFound = true;
                            break;
                        }
                    }

                    Thread.sleep(500);
                }

                while (!boardShown[0]) {
                    Thread.sleep(500);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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

    public static void sendChallenge(GameClient client, String opponentName, String gameName) {
        try {
            List<String> players = client.getPlayerList();
            if (!players.contains(opponentName)) {
                JOptionPane.showMessageDialog(null, "No player found with that name.");
                return;
            }

            System.out.println("DEBUG: Sending challenge to " + opponentName);
            client.challenge(opponentName, gameName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
