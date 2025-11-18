package nl.isy_games;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchHandler {

    public enum Mode { NONE, RANDOM, FIND_PLAYER }

    private static final Map<GameClient, Mode> modes = new ConcurrentHashMap<>();
    private static final Map<GameClient, Boolean> matchStarted = new ConcurrentHashMap<>();
    private static final Map<GameClient, BoardGame> boards = new ConcurrentHashMap<>();
    private static final Map<GameClient, MainFrame> parentFrames = new ConcurrentHashMap<>();
    private static final Set<GameClient> attachedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<GameClient, BoardGame> finishedBoards = new ConcurrentHashMap<>();
    private static final Map<String, String> playerGameMap = new ConcurrentHashMap<>();

    private static final Map<String, Set<GameClient>> randomQueue = new ConcurrentHashMap<>();

    public static void setMode(GameClient client, Mode mode) { modes.put(client, mode); }
    public static void setParentFrame(GameClient client, MainFrame frame) { parentFrames.put(client, frame); }

    public static void setSelectedGameType(String playerName, String gameName) {
        playerGameMap.put(playerName.toLowerCase(), gameName.toLowerCase());
    }

    public static String getSelectedGameType(String playerName) {
        return playerGameMap.get(playerName.toLowerCase());
    }

    private static void handleMatchEnded(GameClient client, String message) {
        if (!Boolean.TRUE.equals(matchStarted.get(client))) return;

        MainFrame frame = parentFrames.get(client);
        if (frame == null) return;

        BoardGame board = boards.remove(client);
        if (board != null) {
            finishedBoards.put(client, board);
            frame.closeGameBoard(board);
        }

        matchStarted.remove(client);
        frame.inMatch = false;

        String comment = parseValue(message, "COMMENT");
        String result = message.contains("WIN") ? "You won!" :
                message.contains("LOSS") ? "You lost!" : "Draw!";
        JOptionPane.showMessageDialog(frame, result + (comment != null ? "\n" + comment : ""));
    }



    public static void attach(GameClient client) {
        if (attachedClients.contains(client)) return;
        attachedClients.add(client);

        client.addServerListener(message -> {
            try {
                if (message.contains("SVR GAME CHALLENGE")) handleChallenge(client, message);
                else if (message.contains("SVR GAME_MATCH_START") || message.contains("SVR GAME MATCH"))
                    handleMatchStart(client, message);
                else if (message.contains("SVR GAME MOVE") || message.contains("SVR GAME YOURTURN")) {
                    BoardGame board = boards.get(client);
                    if (board != null) SwingUtilities.invokeLater(() -> board.updateBoardFromServer(message));
                } else if (message.contains("SVR GAME CANCELLED")) {
                    SwingUtilities.invokeLater(() -> handleOpponentCancelled(client));
                } else if (message.matches(".*SVR GAME (WIN|LOSS|DRAW).*")) {
                    SwingUtilities.invokeLater(() -> handleMatchEnded(client, message));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private static void acceptChallenge(GameClient client, int challengeNumber) {
        System.out.println("[DEBUG] Accepting challenge for " + client.getPlayerName());
        client.acceptChallenge(challengeNumber);
    }

    private static void handleChallenge(GameClient client, String message) {
        String challenger = parseValue(message, "CHALLENGER");
        String challengeNumberStr = parseValue(message, "CHALLENGENUMBER");
        String gameType = parseValue(message, "GAMETYPE");
        if (challenger == null || challengeNumberStr == null) return;
        int challengeNumber = Integer.parseInt(challengeNumberStr);

        Mode mode = modes.getOrDefault(client, Mode.NONE);
        MainFrame frame = parentFrames.get(client);
        if (frame == null) return;

        SwingUtilities.invokeLater(() -> {
            try {
                switch (mode) {
                    case RANDOM:
                        String myGame = getSelectedGameType(client.getPlayerName());
                        if (myGame != null && myGame.equalsIgnoreCase(gameType)) {
                            System.out.println("[DEBUG] RANDOM mode: auto-accepting challenge from " + challenger);
                            acceptChallenge(client, challengeNumber);
                        }
                        break;

                    case FIND_PLAYER:
                        if (frame.getCurrentOpponentName() != null &&
                                frame.getCurrentOpponentName().equalsIgnoreCase(challenger)) {
                            int response = JOptionPane.showConfirmDialog(frame,
                                    challenger + " has challenged you to " + gameType + ". Accept?",
                                    "Challenge", JOptionPane.YES_NO_OPTION);
                            if (response == JOptionPane.YES_OPTION) acceptChallenge(client, challengeNumber);
                            else client.denyChallenge(challengeNumber);
                        } else {
                            client.denyChallenge(challengeNumber);
                        }
                        break;

                    default:
                        int response = JOptionPane.showConfirmDialog(frame,
                                challenger + " has challenged you to " + gameType + ". Accept?",
                                "Challenge", JOptionPane.YES_NO_OPTION);
                        if (response == JOptionPane.YES_OPTION) acceptChallenge(client, challengeNumber);
                        else client.denyChallenge(challengeNumber);
                        break;
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    public static void startRandomMatch(GameClient client, String gameType) {
        System.out.println("[DEBUG] startRandomMatch called for " + client.getPlayerName() + " (" + gameType + ")");
        setSelectedGameType(client.getPlayerName(), gameType);
        setMode(client, Mode.RANDOM);

        randomQueue.putIfAbsent(gameType.toLowerCase(), ConcurrentHashMap.newKeySet());
        Set<GameClient> queue = randomQueue.get(gameType.toLowerCase());

        synchronized (queue) {
            for (GameClient other : queue) {
                if (!other.equals(client)) {
                    System.out.println("[DEBUG] " + client.getPlayerName() + " matched with " + other.getPlayerName());
                    client.challenge(other.getPlayerName(), gameType);
                    queue.remove(other);
                    return;
                }
            }

            queue.add(client);
            System.out.println("[DEBUG] " + client.getPlayerName() + " waiting in random queue for " + gameType);
        }
    }

    private static void handleMatchStart(GameClient client, String message) {
        String opponent = parseValue(message, "OPPONENT");
        String firstPlayer = parseValue(message, "FIRST");
        String playerToMove = parseValue(message, "PLAYERTOMOVE");
        String gameType = parseValue(message, "GAMETYPE"); // <- use server-provided gametype

        if (opponent == null || Boolean.TRUE.equals(matchStarted.get(client))) return;
        matchStarted.put(client, true);

        System.out.println("[DEBUG] Match started for " + client.getPlayerName() + " vs " + opponent + " (" + gameType + ")");

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = parentFrames.get(client);
            if (frame == null) return;

            frame.inMatch = true;

            BoardGame previousBoard = finishedBoards.remove(client);
            if (previousBoard != null) {
                previousBoard.dismissGameOverDialog();
                previousBoard.resetBoardState();
            }

            boolean myTurnFirst = client.getPlayerName().equalsIgnoreCase(firstPlayer != null ? firstPlayer : playerToMove);

            // Determine which board to create based on server-provided game type
            BoardGame board;
            if (gameType != null && gameType.equalsIgnoreCase("reversi")) {
                board = new ReversiGame();  // empty setup
            } else {
                TicTacToeGame tttBoard = new TicTacToeGame(client, myTurnFirst, false, false);
                tttBoard.resetBoardState();
                tttBoard.setGameOverDialogClosedListener(() -> finishedBoards.remove(client));
                tttBoard.setTurn(myTurnFirst ? TicTacToeGame.Turn.LOCAL : TicTacToeGame.Turn.REMOTE);
                if (myTurnFirst) tttBoard.setSymbols("X", "O");
                else tttBoard.setSymbols("O", "X");
                board = tttBoard;
            }

            boards.put(client, board);
            frame.setCurrentOpponentName(opponent);

            board.setCloseCallback(() -> {
                finishedBoards.put(client, board);
                boards.remove(client);
                matchStarted.remove(client);
                frame.inMatch = false;

                client.send("forfeit");
                SwingUtilities.invokeLater(() -> frame.closeGameBoard(board));
            });


            frame.getMainPanel().add(board, "currentGame");
            frame.showCard("currentGame");
            frame.getMainPanel().revalidate();
            frame.getMainPanel().repaint();
            frame.setHeaderLabel("Playing vs " + opponent);

            modes.put(client, Mode.NONE);
            frame.clearCurrentOpponent();
        });
    }

    private static void handleOpponentCancelled(GameClient client) {
        MainFrame frame = parentFrames.get(client);
        if (frame != null) {
            frame.inMatch = false;
            JOptionPane.showMessageDialog(frame, "Your opponent cancelled the match.");

            BoardGame board = boards.remove(client);
            if (board != null) {
                finishedBoards.put(client, board);
                SwingUtilities.invokeLater(() -> frame.closeGameBoard(board));
            }

            matchStarted.remove(client);
            frame.showCard("gameSelector");
            frame.setHeaderLabel("");
            frame.clearCurrentOpponent();
        }
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
