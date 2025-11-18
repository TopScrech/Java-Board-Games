package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private GameClient client;
    private final JPanel mainPanel;
    private final CardLayout cardLayout;

    private JLabel headerLabel;
    private JButton homeButton;
    private JButton logoutButton;

    protected boolean inMatch = false;
    private final Map<GameClient, String> selectedGameType = new HashMap<>();
    private String currentOpponentName;

    public MainFrame() {
        setTitle("Game Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(28, 28, 30));

        mainPanel.add(createLoginPanel(), "login");
        add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 40, 40));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        headerLabel = new JLabel("", SwingConstants.LEFT);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setVisible(false);

        homeButton = createRoundedButton("Home", new Color(0, 123, 255), Color.WHITE);
        homeButton.setVisible(false);
        homeButton.addActionListener(e -> handleHomeClick());

        logoutButton = createRoundedButton("Logout", new Color(220, 53, 69), Color.WHITE);
        logoutButton.setVisible(false);
        logoutButton.addActionListener(e -> handleLogout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(new Color(40, 40, 40));
        leftPanel.add(headerLabel);
        leftPanel.add(homeButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(new Color(40, 40, 40));
        rightPanel.add(logoutButton);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private void handleHomeClick() {
        if (inMatch) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to forfeit the match?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;

            if (client != null) {
                client.send("forfeit");  // correct server command
            }

            // Close current game board immediately
            BoardGame currentBoard = null;
            for (Component comp : mainPanel.getComponents()) {
                if (comp instanceof BoardGame) {
                    currentBoard = (BoardGame) comp;
                    break;
                }
            }
            if (currentBoard != null) closeGameBoard(currentBoard);

            inMatch = false;
        }
        cardLayout.show(mainPanel, "gameSelector");
        headerLabel.setText("");
        headerLabel.setVisible(false);
    }

    private void handleLogout() {
        if (inMatch && client != null) client.send("forfeit");
        inMatch = false;
        client = null;

        mainPanel.removeAll();
        mainPanel.add(createLoginPanel(), "login");
        cardLayout.show(mainPanel, "login");
        headerLabel.setText("");
        headerLabel.setVisible(false);
        homeButton.setVisible(false);
        logoutButton.setVisible(false);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(28, 28, 30));

        JPanel loginBox = new JPanel();
        loginBox.setLayout(new BoxLayout(loginBox, BoxLayout.Y_AXIS));
        loginBox.setBackground(new Color(40, 40, 40));
        loginBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        loginBox.setPreferredSize(new Dimension(350, 260));

        JLabel title = new JLabel("Enter your player name:", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBox.add(title);
        loginBox.add(Box.createVerticalStrut(20));

        JTextField nameField = new JTextField();
        nameField.setBackground(new Color(60, 60, 60));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);
        nameField.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBox.add(nameField);
        loginBox.add(Box.createVerticalStrut(15));

        JButton loginButton = createRoundedButton("Login", new Color(0, 123, 255), Color.WHITE);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBox.add(loginButton);
        loginBox.add(Box.createVerticalStrut(15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginBox, gbc);

        loginButton.addActionListener(e -> {
            String playerName = nameField.getText().trim().toLowerCase();
            if (playerName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a valid name.");
                return;
            }

            loginButton.setEnabled(false);

            try {
                client = new GameClient("5.83.140.43", 7789, playerName);
                new Thread(client::startListening).start();
                client.login();

                MatchHandler.attach(client);
                MatchHandler.setParentFrame(client, this);

                mainPanel.add(createGameSelectorPanel(), "gameSelector");
                cardLayout.show(mainPanel, "gameSelector");

                homeButton.setVisible(true);
                logoutButton.setVisible(true);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage());
                loginButton.setEnabled(true);
            }
        });

        return panel;
    }

    private JPanel createGameSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(new Color(28, 28, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        JLabel welcomeLabel = new JLabel("Welcome, " + client.getPlayerName() + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeLabel.setForeground(Color.WHITE);
        panel.add(welcomeLabel, BorderLayout.NORTH);

        JPanel gameGrid = new JPanel(new GridLayout(0, 5, 20, 20));
        gameGrid.setBackground(new Color(28, 28, 30));

        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                Color bgColor;
                String iconPath;
                switch (gameName.toLowerCase()) {
                    case "tic-tac-toe":
                        bgColor = new Color(191, 50, 159);
                        iconPath = "/icons/TicTacToe.png";
                        break;
                    case "connect-four":
                    case "connect4":
                        bgColor = new Color(255, 193, 7);
                        iconPath = "/icons/Connect4.png";
                        break;
                    case "reversi":
                    case "othello":
                        bgColor = new Color(69, 255, 7);
                        iconPath = "/icons/Othello.png";
                        break;
                    case "battleship":
                        bgColor = new Color(106, 70, 194);
                        iconPath = "/icons/Battleship.png";
                        break;
                    default:
                        bgColor = new Color(0, 123, 255);
                        iconPath = "/icons/TicTacToe.png";
                }

                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(bgColor);
                card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));

                URL iconURL = getClass().getResource(iconPath);
                JLabel iconLabel = new JLabel();
                iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                if (iconURL != null) iconLabel.setIcon(new ImageIcon(iconURL));
                else {
                    iconLabel.setText(gameName);
                    iconLabel.setForeground(Color.WHITE);
                    iconLabel.setFont(new Font("Arial", Font.BOLD, 16));
                }
                card.add(iconLabel, BorderLayout.CENTER);

                JLabel nameLabel = new JLabel(gameName, SwingConstants.CENTER);
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
                card.add(nameLabel, BorderLayout.SOUTH);

                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        mainPanel.add(createGameModePanel(gameName), "gameMode");
                        cardLayout.show(mainPanel, "gameMode");
                        headerLabel.setText(gameName);
                        headerLabel.setVisible(true);
                    }

                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        card.setBackground(bgColor.darker());
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        card.setBackground(bgColor);
                    }
                });

                gameGrid.add(card);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not fetch gamelist: " + e.getMessage());
        }

        JScrollPane scrollPane = new JScrollPane(gameGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(28, 28, 30));
        scrollPane.getViewport().setBackground(new Color(28, 28, 30));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createGameModePanel(String gameName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(28, 28, 30));

        JLabel label = new JLabel("Choose mode for " + gameName + ":", SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(20));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonsPanel.setBackground(new Color(28, 28, 30));

        JButton vsPlayerBtn = createRoundedButton("Play vs Player", new Color(0, 123, 255), Color.WHITE);
        vsPlayerBtn.setPreferredSize(new Dimension(180, 80));
        JButton vsAIBtn = createRoundedButton("Play vs AI", new Color(255, 193, 7), Color.WHITE);
        vsAIBtn.setPreferredSize(new Dimension(180, 80));

        buttonsPanel.add(vsPlayerBtn);
        buttonsPanel.add(vsAIBtn);
        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(20));

        JPanel pvpOptionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pvpOptionsPanel.setBackground(new Color(28, 28, 30));

        JButton randomBtn = createRoundedButton("Random", new Color(0, 123, 255), Color.WHITE);
        randomBtn.setPreferredSize(new Dimension(140, 60));
        JButton findBtn = createRoundedButton("Find Player", new Color(255, 193, 7), Color.WHITE);
        findBtn.setPreferredSize(new Dimension(140, 60));
        randomBtn.setVisible(false);
        findBtn.setVisible(false);

        pvpOptionsPanel.add(randomBtn);
        pvpOptionsPanel.add(findBtn);
        panel.add(pvpOptionsPanel);
        panel.add(Box.createVerticalStrut(20));

        JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);

        vsAIBtn.addActionListener(e -> startAIMode(gameName));

        vsPlayerBtn.addActionListener(e -> {
            randomBtn.setVisible(true);
            findBtn.setVisible(true);
            statusLabel.setText("Choose Random or Find Player...");
        });

        randomBtn.addActionListener(e -> {
            randomBtn.setVisible(false);
            findBtn.setVisible(false);
            statusLabel.setText("Searching for a random opponent...");
            statusLabel.setForeground(Color.YELLOW);

            String gameType = gameName;
            MatchHandler.setSelectedGameType(client.getPlayerName(), gameType);
            MatchHandler.setMode(client, MatchHandler.Mode.RANDOM);

            try {
                client.subscribe(gameType);
            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Error subscribing for random match.");
                statusLabel.setForeground(Color.RED);
            }
        });


        findBtn.addActionListener(e -> {
            randomBtn.setVisible(false);
            findBtn.setVisible(false);

            String opponentName = JOptionPane.showInputDialog(this, "Enter player name:");
            if (opponentName == null || opponentName.isEmpty()) return;

            try {
                List<String> players = client.getPlayerList();
                boolean found = players.stream().anyMatch(p -> p.equalsIgnoreCase(opponentName));
                if (!found) {
                    JOptionPane.showMessageDialog(this, "Player not found.");
                    return;
                }

                MatchHandler.setSelectedGameType(client.getPlayerName(), gameName);
                MatchHandler.setMode(client, MatchHandler.Mode.FIND_PLAYER);
                setCurrentOpponentName(opponentName);

                client.challenge(opponentName, gameName);
                statusLabel.setText("Invitation sent to " + opponentName);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return panel;
    }

    private void startAIMode(String gameName) {
        BoardGame gamePanel;
        switch (gameName.toLowerCase()) {
            case "tic-tac-toe":
                gamePanel = new TicTacToeGame(null, true, true);
                break;
            case "reversi":
            case "othello":
                gamePanel = new ReversiGame();
                break;
            default:
                JOptionPane.showMessageDialog(this, "Game not implemented yet.");
                return;
        }

        inMatch = true;
        mainPanel.add(gamePanel, "currentGame");
        showCard("currentGame");
        setHeaderLabel("Playing " + gameName);
    }

    public void closeGameBoard(BoardGame board) {
        if (board != null) {
            mainPanel.remove(board);
        }
        inMatch = false;
        showCard("gameSelector");
        setHeaderLabel("");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private JButton createRoundedButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(bgColor, 2));
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(150, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void showCard(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void setHeaderLabel(String text) {
        headerLabel.setText(text);
        headerLabel.setVisible(!text.isEmpty());
    }

    public String getCurrentOpponentName() {
        return currentOpponentName;
    }

    public void setCurrentOpponentName(String name) {
        currentOpponentName = name;
    }

    public void clearCurrentOpponent() {
        currentOpponentName = null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}

