package classes;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchHandler {

    public enum Mode { NONE, RANDOM, FIND_PLAYER }

    private static final Map<GameClient, Mode> modes = new ConcurrentHashMap<>();
    private static final Map<GameClient, Boolean> matchStarted = new ConcurrentHashMap<>();
    private static final Map<GameClient, TicTacToeGame> boards = new ConcurrentHashMap<>();
    private static final Map<GameClient, MainFrame> parentFrames = new ConcurrentHashMap<>();
    private static final Set<GameClient> attachedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setMode(GameClient client, Mode mode) { modes.put(client, mode); }
    public static void setParentFrame(GameClient client, MainFrame frame) { parentFrames.put(client, frame); }

    public static void attach(GameClient client) {
        if (attachedClients.contains(client)) return;
        attachedClients.add(client);

        client.addServerListener(message -> {
            try {
                if (message.contains("SVR GAME CHALLENGE")) handleChallenge(client,message);
                else if (message.contains("SVR GAME MATCH")) handleMatchStart(client,message);
                else if (message.contains("SVR GAME MOVE") || message.contains("SVR GAME YOURTURN")) {
                    TicTacToeGame board = boards.get(client);
                    if (board != null) SwingUtilities.invokeLater(() -> board.updateBoardFromServer(message));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private static void handleChallenge(GameClient client, String message) {
        String challenger = parseValue(message,"CHALLENGER");
        String challengeNumberStr = parseValue(message,"CHALLENGENUMBER");
        if (challenger==null||challengeNumberStr==null) return;
        int challengeNumber = Integer.parseInt(challengeNumberStr);

        Mode mode = modes.getOrDefault(client, Mode.NONE);
        MainFrame frame = parentFrames.get(client);
        if (frame==null) return;

        SwingUtilities.invokeLater(() -> {
            try {
                switch(mode){
                    case RANDOM: client.acceptChallenge(challengeNumber); break;
                    case FIND_PLAYER:
                        if(frame.getCurrentOpponentName()!=null &&
                                frame.getCurrentOpponentName().equalsIgnoreCase(challenger))
                            client.acceptChallenge(challengeNumber);
                        break;
                    default:
                        int response = JOptionPane.showConfirmDialog(frame, challenger+" has challenged you to TicTacToe. Accept?", "Challenge", JOptionPane.YES_NO_OPTION);
                        if(response==JOptionPane.YES_OPTION) client.acceptChallenge(challengeNumber);
                        else client.denyChallenge(challengeNumber);
                        break;
                }
            } catch(Exception ex){ ex.printStackTrace(); }
        });
    }

    private static void handleMatchStart(GameClient client, String message) {
        String opponent = parseValue(message, "OPPONENT");
        String firstPlayer = parseValue(message, "FIRST");

        if (opponent == null) return;
        if (Boolean.TRUE.equals(matchStarted.get(client))) return;
        matchStarted.put(client, true);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = parentFrames.get(client);
            if (frame == null) return;

            boolean myTurnFirst = firstPlayer != null && !firstPlayer.isEmpty()
                    ? client.getPlayerName().equalsIgnoreCase(firstPlayer)
                    : client.getPlayerName().compareToIgnoreCase(opponent) < 0;

            TicTacToeGame board = new TicTacToeGame(client, false, false); // PvP
            board.setTurn(myTurnFirst ? TicTacToeGame.Turn.LOCAL : TicTacToeGame.Turn.REMOTE);

            if (myTurnFirst) board.setSymbols("X","O");
            else board.setSymbols("O","X");

            boards.put(client, board);
            frame.setCurrentOpponentName(opponent);

            board.setCloseCallback(() -> {
                boards.remove(client);
                matchStarted.remove(client);
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

    private static String parseValue(String message,String key){
        try{
            int idx=message.indexOf(key+":");
            if(idx<0) return null;
            int start = message.indexOf("\"",idx)+1;
            int end = message.indexOf("\"",start);
            return message.substring(start,end);
        }catch(Exception e){ return null; }
    }
}
