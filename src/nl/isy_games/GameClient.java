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

    public void login() throws IOException {
        send("login " + playerName);
        String response;
        while ((response = in.readLine()) != null) {
            System.out.println("DEBUG Server response: " + response);
            if (response.startsWith("OK")) {
                System.out.println("DEBUG: Login geslaagd");
                return;
            } else if (response.startsWith("ERR")) {
                System.out.println("DEBUG: Login mislukt");
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

    public void listen() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            System.out.println("DEBUG Server message: " + message);
            if (serverListener != null) serverListener.handleMessage(message);
        }
    }

    public List<String> getPlayerList() throws IOException {
        send("get playerlist");
        System.out.println("DEBUG: get playerlist verzonden");

        String ok = in.readLine();
        System.out.println("DEBUG: playerlist OK response: " + ok);
        String listLine = in.readLine();
        System.out.println("DEBUG: playerlist data: " + listLine);

        List<String> players = new ArrayList<>();
        if (listLine != null && listLine.startsWith("SVR PLAYERLIST")) {
            int start = listLine.indexOf('[');
            int end = listLine.indexOf(']');
            if (start >= 0 && end > start) {
                String list = listLine.substring(start + 1, end);
                String[] names = list.replaceAll("\"", "").split(",");
                for (String name : names) if (!name.trim().isEmpty()) players.add(name.trim());
            }
        }

        System.out.println("DEBUG: Parsed players: " + players);
        return players;
    }

    public List<String> getGameList() throws IOException {
        send("get gamelist");
        System.out.println("DEBUG: get gamelist verzonden");

        String ok = in.readLine();
        System.out.println("DEBUG: gamelist OK response: " + ok);
        if (ok == null || !ok.startsWith("OK")) {
            throw new IOException("Kon gamelist niet ophalen, server antwoordde: " + ok);
        }

        String listLine = in.readLine();
        System.out.println("DEBUG: gamelist data: " + listLine);
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

        System.out.println("DEBUG: Parsed games: " + games);
        return games;
    }


    private void send(String msg) {
        out.println(msg);
        System.out.println("DEBUG Sent: " + msg);
    }

    public void close() throws IOException {
        send("logout");
        socket.close();
        System.out.println("DEBUG: Verbinding gesloten");
    }

    public String getPlayerName() {
        return playerName;
    }

    public interface ServerListener {
        void handleMessage(String message);
    }
}
