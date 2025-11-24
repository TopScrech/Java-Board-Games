package nl.isy_games;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameClient {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final String playerName;

    private final List<ServerListener> serverListeners = new ArrayList<>();
    private Thread listenerThread;

    public GameClient(String host, int port, String playerName) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.playerName = playerName;
        System.out.println("DEBUG: Connected to server " + host + ":" + port);
    }

    public void addServerListener(ServerListener listener) {
        serverListeners.add(listener);
    }

    public void startListening() {
        // Prevent multiple listener threads
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            try {
                String message;
                while (!socket.isClosed()) {
                    // Wait for data only when available
                    if (in.ready()) {
                        message = in.readLine();
                        if (message == null) break;
                        message = message.trim();
                        if (message.isEmpty()) continue;

                        System.out.println("DEBUG Server message: " + message);

                        synchronized (serverListeners) {
                            for (ServerListener listener : serverListeners) {
                                try {
                                    listener.handleMessage(message);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        // No message yet → sleep a bit to avoid CPU spam
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {}
                    }
                }
            } catch (IOException e) {
                System.err.println("Server listener stopped: " + e.getMessage());
            }
        }, "ServerListenerThread");

        listenerThread.setDaemon(true); // won’t block program exit
        listenerThread.start();
    }


    public void login() throws IOException {
        send("login " + playerName);
        String response;

        while ((response = in.readLine()) != null) {
            System.out.println("DEBUG Server response: " + response);
            if (response.startsWith("OK")) return;
            else if (response.startsWith("ERR")) throw new IOException("Login failed: " + response);
        }

        throw new IOException("Login failed: no response from server");
    }

    public void subscribe(String gameType) {
        send("subscribe " + gameType);
        System.out.println("DEBUG: Subscribed to " + gameType);
    }

    public List<String> getPlayerList() throws IOException {
        send("get playerlist");
        String ok = in.readLine();
        String listLine = in.readLine();

        List<String> players = new ArrayList<>();

        if (listLine != null && listLine.startsWith("SVR PLAYERLIST")) {
            int start = listLine.indexOf('[');
            int end = listLine.indexOf(']');

            if (start >= 0 && end > start) {
                String list = listLine.substring(start + 1, end);
                String[] names = list.replaceAll("\"", "").split(",");

                for (String name : names)
                    if (!name.trim().isEmpty())
                        players.add(name.trim());
            }
        }

        return players;
    }

    public List<String> getGameList() throws IOException {
        send("get gamelist");
        String ok = in.readLine();
        String listLine = in.readLine();

        List<String> games = new ArrayList<>();

        if (listLine != null && listLine.startsWith("SVR GAMELIST")) {
            int start = listLine.indexOf('[');
            int end = listLine.lastIndexOf(']');

            if (start >= 0 && end > start) {
                String list = listLine.substring(start + 1, end);
                String[] items = list.split(",");

                for (String game : items) {
                    game = game.replaceAll("\"", "").trim();
                    if (!game.isEmpty()) games.add(game);
                }
            }
        }
        return games;
    }

    public void challenge(String opponent, String gameType) {
        send("challenge \"" + opponent + "\" \"" + gameType + "\"");
        System.out.println("DEBUG: Challenge sent to " + opponent + " for " + gameType);
    }

    public void acceptChallenge(int challengeNumber) {
        send("challenge accept " + challengeNumber);
        System.out.println("DEBUG: Challenge accepted #" + challengeNumber);
    }

    public void denyChallenge(int challengeNumber) {
        send("challenge deny " + challengeNumber);
        System.out.println("DEBUG: Challenge denied #" + challengeNumber);
    }

    void send(String msg) {
        out.println(msg);
        System.out.println("DEBUG Sent: " + msg);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void sendMove(int move) {
        send("move " + move);
        System.out.println("DEBUG: Move sent: " + move);
    }

    public interface ServerListener {
        void handleMessage(String message);
    }
}
