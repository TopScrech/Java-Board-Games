package classes;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchHandler {

    public enum Mode { NONE, RANDOM, FIND_PLAYER }

    private static final Map<GameClient, Mode> modes = new ConcurrentHashMap<>();
    private static final Map<GameClient, Set<Integer>> handledChallenges = new ConcurrentHashMap<>();
    private static final Map<GameClient, Set<String>> handledMatches = new ConcurrentHashMap<>();
    private static final Map<GameClient, Boolean> matchStarted = new ConcurrentHashMap<>();
    private static final Map<GameClient, TicTacToeGame> boards = new ConcurrentHashMap<>();
    private static final Map<GameClient, JFrame> parentFrames = new ConcurrentHashMap<>();
    private static final Set<GameClient> attachedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setMode(GameClient client, Mode mode) {
        modes.put(client, mode);
    }

    public static void setParentFrame(GameClient client, JFrame frame) {
        parentFrames.put(client, frame);
    }

    public static void attach(GameClient client) {
        if (attachedClients.contains(client)) return;
        attachedClients.add(client);

        client.addServerListener(message -> {
            try {
                if (message.contains("SVR GAME CHALLENGE")) {
                    String challenger = parseValue(message, "CHALLENGER");
                    String challengeNumberStr = parseValue(message, "CHALLENGENUMBER");
                    String gameType = parseValue(message, "GAMETYPE");

                    if (challenger == null || challengeNumberStr == null) return;
                    int challengeNumber;
                    try { challengeNumber = Integer.parseInt(challengeNumberStr); }
                    catch (NumberFormatException ex) { return; }

                    handledChallenges.computeIfAbsent(client, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    if (handledChallenges.get(client).contains(challengeNumber)) return;
                    handledChallenges.get(client).add(challengeNumber);

                    if (challenger.equalsIgnoreCase(client.getPlayerName())) return;

                    Mode mode = modes.getOrDefault(client, Mode.NONE);

                    if (mode == Mode.RANDOM) {
                        System.out.println("DEBUG: Auto-accept random challenge from " + challenger);
                        client.acceptChallenge(challengeNumber);
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        JFrame parent = parentFrames.getOrDefault(client, null);
                        int response = JOptionPane.showConfirmDialog(
                                parent,
                                challenger + " has challenged you to " + gameType + ". Accept?",
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
                }

                else if (message.contains("SVR GAME MATCH")) {
                    String opponent = parseValue(message, "OPPONENT");
                    if (opponent == null) return;

                    handledMatches.computeIfAbsent(client, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    if (handledMatches.get(client).contains(opponent)) return;
                    handledMatches.get(client).add(opponent);

                    if (Boolean.TRUE.equals(matchStarted.get(client))) return;
                    matchStarted.put(client, true);

                    SwingUtilities.invokeLater(() -> {
                        TicTacToeGame existing = boards.get(client);
                        if (existing != null && existing.isDisplayable()) {
                            System.out.println("DEBUG: Board already open, skipping duplicate.");
                            return;
                        }

                        TicTacToeGame newBoard = new TicTacToeGame(client);
                        newBoard.setVisible(true);
                        boards.put(client, newBoard);
                        System.out.println("DEBUG: Match started with " + opponent);

                        newBoard.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                resetClient(client);
                                boards.remove(client);
                                System.out.println("DEBUG: Board closed — state reset.");
                            }
                        });
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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

    private static void resetClient(GameClient client) {
        handledChallenges.remove(client);
        handledMatches.remove(client);
        matchStarted.put(client, false);
        modes.put(client, Mode.NONE);
    }
}
