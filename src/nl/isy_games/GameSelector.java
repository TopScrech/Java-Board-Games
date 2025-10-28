package nl.isy_games;

import javax.swing.*;

import nl.isy_games.GameClient;
import nl.isy_games.GameModeSelector;
import nl.isy_games.TicTacToeGame;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameSelector extends JFrame {

    private final GameClient client;

    public GameSelector(GameClient client) {
        this.client = client;

        setTitle("Game Selector");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Kies een spel:", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        add(label, BorderLayout.NORTH);

        JPanel gamePanel = new JPanel(new GridLayout(0, 1, 10, 10));
        add(new JScrollPane(gamePanel), BorderLayout.CENTER);

        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                JButton button = new JButton(gameName);
                button.setFont(new Font("Arial", Font.PLAIN, 14));
                button.addActionListener(e -> new GameModeSelector(client, gameName));
                gamePanel.add(button);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Kon geen gamelist ophalen: " + e.getMessage(),
                    "Fout", JOptionPane.ERROR_MESSAGE);
        }

        setVisible(true);
    }
}
