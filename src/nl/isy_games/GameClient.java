package classes;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;

    private ServerListener serverListener;
    private Thread listenerThread;

    public GameClient(String host, int port, String playerName) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.playerName = playerName;
        System.out.println("DEBUG: Verbonden met server " + host + ":" + port);
    }

    public void setServerListener(ServerListener listener) {
        this.serverListener = listener;
    }

    public void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("DEBUG Server message: " + message);
                    if (serverListener != null) serverListener.handleMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenerThread.start();
    }

    public void login() throws IOException {
        send("login " + playerName);
        String response;
        while ((response = in.readLine()) != null) {
            System.out.println("DEBUG Server response: " + response);
            if (response.startsWith("OK")) {
                System.out.println("DEBUG: Login geslaagd");
                return;
            } else if (response.startsWith("ERR")) {
                throw new IOException("Login failed: " + response);
            }
        }
        throw new IOException("Login failed: geen antwoord van server");
    }

    public void subscribe(String gameType) {
        send("subscribe " + gameType);
        System.out.println("DEBUG: Subscribed to " + gameType);
    }

    public void sendMove(int move) {
        send("move " + move);
        System.out.println("DEBUG: Move verzonden: " + move);
    }

    public void forfeit() {
        send("forfeit");
        System.out.println("DEBUG: Match opgegeven");
    }

    public void logout() {
        send("logout");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        if (listLine == null || !listLine.startsWith("SVR GAMELIST")) {
            throw new IOException("Ongeldig gamelist response van server: " + listLine);
        }

        int start = listLine.indexOf('[');
        int end = listLine.lastIndexOf(']');
        List<String> games = new ArrayList<>();
        if (start >= 0 && end > start) {
            String list = listLine.substring(start + 1, end);
            String[] items = list.split(",");
            for (String game : items) {
                game = game.replaceAll("\"", "").trim();
                if (!game.isEmpty()) games.add(game);
            }
        }
        return games;
    }

    private void send(String msg) {
        out.println(msg);
        System.out.println("DEBUG Sent: " + msg);
    }

    public String getPlayerName() {
        return playerName;
    }

    public interface ServerListener {
        void handleMessage(String message);
    }

    public void listen() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            System.out.println("DEBUG Server message: " + message);
            if (serverListener != null) {
                serverListener.handleMessage(message);
            }
        }
    }

    public void challenge(String opponent, String gameType) {
        send("challenge \"" + opponent + "\" \"" + gameType + "\"");
        System.out.println("DEBUG: Challenge sent to " + opponent + " for game " + gameType);
    }

    public void acceptChallenge(int challengeNumber) {
        send("challenge accept " + challengeNumber);
        System.out.println("DEBUG: Challenge accepted #" + challengeNumber);
    }
}
