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
    private static final Map<GameClient, MainFrame> parentFrames = new ConcurrentHashMap<>();
    private static final Set<GameClient> attachedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setMode(GameClient client, Mode mode) {
        modes.put(client, mode);
    }

    public static void setParentFrame(GameClient client, MainFrame frame) {
        parentFrames.put(client, frame);
    }

    public static void attach(GameClient client) {
        if (attachedClients.contains(client)) return;
        attachedClients.add(client);

        client.addServerListener(message -> {
            try {
                if (message.contains("SVR GAME CHALLENGE")) handleChallenge(client, message);
                else if (message.contains("SVR GAME MATCH")) handleMatchStart(client, message);
                else if (message.contains("SVR GAME YOURTURN") || message.contains("SVR GAME MOVE")) {
                    TicTacToeGame board = boards.get(client);
                    if (board != null) SwingUtilities.invokeLater(() -> board.updateBoardFromServer(message));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private static void handleChallenge(GameClient client, String message) {
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

        SwingUtilities.invokeLater(() -> {
            try {
                switch (mode) {
                    case RANDOM:
                        client.acceptChallenge(challengeNumber);
                        break;

                    case FIND_PLAYER:
                        MainFrame frame = parentFrames.get(client);
                        if (frame != null && frame.getCurrentOpponentName() != null &&
                                frame.getCurrentOpponentName().equalsIgnoreCase(challenger)) {
                            client.acceptChallenge(challengeNumber);
                        }
                        break;

                    default:
                        MainFrame mainFrame = parentFrames.get(client);
                        if (mainFrame == null) return;

                        int response = JOptionPane.showConfirmDialog(
                                mainFrame,
                                challenger + " has challenged you to " + gameType + ". Accept?",
                                "Incoming Challenge",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (response == JOptionPane.YES_OPTION) client.acceptChallenge(challengeNumber);
                        else client.denyChallenge(challengeNumber);
                        break;
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private static void handleMatchStart(GameClient client, String message) {
        String opponent = parseValue(message, "OPPONENT");
        String firstPlayer = parseValue(message, "FIRST");
        String gameType = parseValue(message, "GAMETYPE");

        if (opponent == null || gameType == null) return;
        if (Boolean.TRUE.equals(matchStarted.get(client))) return;
        matchStarted.put(client, true);

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = parentFrames.get(client);
            if (mainFrame == null) return;

            if (boards.get(client) != null) return;

            boolean myTurnFirst;
            if (firstPlayer != null && !firstPlayer.isEmpty()) {
                myTurnFirst = client.getPlayerName().equalsIgnoreCase(firstPlayer);
            } else {
                myTurnFirst = client.getPlayerName().compareToIgnoreCase(opponent) < 0;
            }

            String mySymbol = myTurnFirst ? "X" : "O";
            String opponentSymbol = myTurnFirst ? "O" : "X";

            TicTacToeGame newBoard = new TicTacToeGame(client, gameType, mySymbol, opponentSymbol, myTurnFirst);
            boards.put(client, newBoard);

            mainFrame.getMainPanel().add(newBoard, "currentGame");
            mainFrame.showCard("currentGame");
            mainFrame.setHeaderLabel("Playing vs " + opponent);

            modes.put(client, Mode.NONE);
            mainFrame.clearCurrentOpponent();
        });
    }


    private static String parseValue(String message, String key) {
        try {
            int idx = message.indexOf(key + ":");
            if (idx < 0) return null;
            int start = message.indexOf("\"", idx) + 1;
            int end = message.indexOf("\"", start);
            return message.substring(start, end);
        } catch (Exception e) { return null; }
    }
}
