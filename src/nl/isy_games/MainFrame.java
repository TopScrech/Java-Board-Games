package nl.isy_games;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.border.EmptyBorder;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class MainFrame extends JFrame {

    private GameClient client;
    private final JPanel mainPanel;
    private final CardLayout cardLayout;

    private JLabel headerLabel;
    private JButton homeButton;
    private JButton playerButton;
    private JButton logoutButton;

    private JPanel sideMenu;
    private JPanel headerPanel;

    protected boolean inMatch = false;
    private final Map<GameClient, String> selectedGameType = new HashMap<>();
    private String currentOpponentName;

    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> sortCombo;
    private JPanel gameGrid;
    private List<GameData> allGames = new ArrayList<>();

    public MainFrame() {
        setTitle("Game Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 🔥 HIDE SIDEBAR + HEADER AT STARTUP
        sideMenu.setVisible(false);
        headerPanel.setVisible(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(28, 28, 30));

        mainPanel.add(createLoginPanel(), "login");
        add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }


    private JPanel createSidebar() {
        sideMenu = new JPanel();
        sideMenu.setLayout(new BoxLayout(sideMenu, BoxLayout.Y_AXIS));
        sideMenu.setBackground(new Color(28, 28, 30));
        sideMenu.setPreferredSize(new Dimension(220, getHeight()));
        sideMenu.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        sideMenu.add(Box.createVerticalStrut(20));

        // Navigation items
        String[][] navItems = {
                {"🏠", "Home"},
                {"📊", "Dashboard"},
                {"🎮", "Games"},
                {"🏆", "Leaderboard"},
                {"⚙️", "Settings"}
        };

        for (int i = 0; i < navItems.length; i++) {
            sideMenu.add(createNavItem(navItems[i][0], navItems[i][1], i == 0));
        }

        return sideMenu;
    }

    private JPanel createNavItem(String icon, String text, boolean active) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        item.setBackground(active ? new Color(58, 58, 60) : new Color(28, 28, 30));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (active) {
            item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0, 123, 255)),
                    BorderFactory.createEmptyBorder(0, 17, 0, 0)
            ));
        } else {
            item.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        }

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconLabel.setForeground(active ? Color.WHITE : new Color(171, 171, 171));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        textLabel.setForeground(active ? Color.WHITE : new Color(171, 171, 171));

        item.add(iconLabel);
        item.add(textLabel);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!active) {
                    item.setBackground(new Color(58, 58, 60));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!active) {
                    item.setBackground(new Color(28, 28, 30));
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (text.equals("Settings")){
                    openSettingsPage();
                } else if (text.equals("Home")) {
                    handleHomeClick();
                } else {
                    handleHomeClick();
                }
            }
        });

        return item;
    }

    private JPanel createHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(40, 40, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        headerLabel = new JLabel("", SwingConstants.LEFT);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setVisible(false);

        homeButton = createRoundedButton("Home", new Color(0, 123, 255), Color.WHITE);
        homeButton.setVisible(false);
        homeButton.addActionListener(e -> handleHomeClick());

        playerButton = createRoundedButton("Player", new Color(100, 100, 100), Color.WHITE);
        playerButton.setVisible(false);
        playerButton.addActionListener(e -> handlePlayerClick());

        logoutButton = createRoundedButton("Logout", new Color(220, 53, 69), Color.WHITE);
        logoutButton.setVisible(false);
        logoutButton.addActionListener(e -> handleLogout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(new Color(40, 40, 40));
        leftPanel.add(headerLabel);
        leftPanel.add(homeButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(new Color(40, 40, 40));
        rightPanel.add(playerButton);
        rightPanel.add(logoutButton);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private void handlePlayerClick() {
        // Add your player profile logic here
        if (client != null) {
            JOptionPane.showMessageDialog(this,
                    "Player: " + client.getPlayerName() + "\nStatus: " + (inMatch ? "In Match" : "Available"),
                    "Player Profile",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }


    private void handleHomeClick() {
        if (inMatch) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to forfeit the match?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;

            if (client != null) {
                client.send("forfeit");
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

    private void openSettingsPage() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBackground(new Color(28, 28, 30));
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Settings", SwingConstants.LEFT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        settingsPanel.add(title);

        settingsPanel.add(Box.createVerticalStrut(20));

        mainPanel.add(settingsPanel, "settings");
        showCard("settings");
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
        playerButton.setVisible(false);
        logoutButton.setVisible(false);
        add(createSidebar(), BorderLayout.WEST);
        add(createHeaderPanel(), BorderLayout.NORTH);

        sideMenu.setVisible(false);
        headerPanel.setVisible(false);


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
                playerButton.setVisible(true);
                playerButton.setText(playerName);

                sideMenu.setVisible(true);
                headerPanel.setVisible(true);


            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage());
                loginButton.setEnabled(true);
            }
        });

        return panel;
    }

    private JPanel createGameSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(new Color(28, 28, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        // Top section with search and filters
        JPanel topSection = new JPanel(new BorderLayout(10, 10));
        topSection.setBackground(new Color(28, 28, 30));

        // Title
        JLabel titleLabel = new JLabel("All Games");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterBar.setBackground(new Color(28, 28, 30));

        // Category dropdown
        JLabel categoryLabel = new JLabel("Categories");
        categoryLabel.setForeground(new Color(150, 150, 150));
        categoryLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        String[] categories = {"All Games", "Strategy", "Puzzle", "Board", "Action"};
        categoryCombo = createStyledComboBox(categories);
        categoryCombo.addActionListener(e -> filterGames());

        // Sort dropdown
        JLabel sortLabel = new JLabel("Sort by");
        sortLabel.setForeground(new Color(150, 150, 150));
        sortLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        String[] sortOptions = {"Name", "Popularity", "Recently Added"};
        sortCombo = createStyledComboBox(sortOptions);
        sortCombo.addActionListener(e -> filterGames());

        // Search field
        searchField = createStyledSearchField();
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterGames(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterGames(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterGames(); }
        });

        // Add filter icon buttons
        JButton filterBtn1 = createFilterIconButton("🔍");
        JButton filterBtn2 = createFilterIconButton("⭐");

        filterBar.add(categoryLabel);
        filterBar.add(categoryCombo);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(sortLabel);
        filterBar.add(sortCombo);
        filterBar.add(Box.createHorizontalStrut(20));
        filterBar.add(searchField);
        filterBar.add(filterBtn1);
        filterBar.add(filterBtn2);

        topSection.add(titleLabel, BorderLayout.NORTH);
        topSection.add(filterBar, BorderLayout.CENTER);

        panel.add(topSection, BorderLayout.NORTH);

        // Game grid with scroll
        gameGrid = new JPanel(new GridBagLayout());
        gameGrid.setBackground(new Color(28, 28, 30));


        loadGames();
        displayGames(allGames);

        JScrollPane scrollPane = createStyledScrollPane(gameGrid);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JTextField createStyledSearchField() {
        JTextField field = new JTextField(20);
        field.setBackground(new Color(40, 40, 45));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(250, 35));

        // Placeholder text
        field.setText("Search games...");
        field.setForeground(new Color(120, 120, 120));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals("Search games...")) {
                    field.setText("");
                    field.setForeground(Color.WHITE);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText("Search games...");
                    field.setForeground(new Color(120, 120, 120));
                }
            }
        });

        return field;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(new Color(40, 40, 45));
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
        combo.setFont(new Font("Arial", Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(150, 35));
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Custom renderer for dropdown items
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(60, 60, 70) : new Color(40, 40, 45));
                setForeground(Color.WHITE);
                setBorder(new EmptyBorder(5, 10, 5, 10));
                return this;
            }
        });

        return combo;
    }

    private JButton createFilterIconButton(String icon) {
        JButton btn = new JButton(icon);
        btn.setBackground(new Color(40, 40, 45));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
        btn.setPreferredSize(new Dimension(35, 35));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(60, 60, 70));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(40, 40, 45));
            }
        });

        return btn;
    }

    private JScrollPane createStyledScrollPane(JPanel content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(28, 28, 30));
        scrollPane.getViewport().setBackground(new Color(28, 28, 30));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Custom scrollbar styling
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(80, 80, 85);
                this.trackColor = new Color(28, 28, 30);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });

        return scrollPane;
    }

    private void loadGames() {
        allGames.clear();

        try {
            List<String> games = client.getGameList();
            for (String gameName : games) {
                GameData gameData = createGameData(gameName);
                allGames.add(gameData);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not fetch game list: " + e.getMessage());
        }
    }

    private GameData createGameData(String gameName) {
        Color bgColor;
        String iconPath;
        String category;

        switch (gameName.toLowerCase()) {
            case "connect-four":
            case "connect4":
                bgColor = new Color(255, 165, 0);
                iconPath = "/icons/Connect4.png";
                category = "Strategy";
                break;
            case "reversi":
            case "othello":
                bgColor = new Color(100, 100, 100);
                iconPath = "/icons/Othello.png";
                category = "Strategy";
                break;
            case "battleship":
                bgColor = new Color(0, 150, 200);
                iconPath = "/icons/Battleship.png";
                category = "Strategy";
                break;
            case "tic-tac-toe":
                bgColor = new Color(191, 50, 159);
                iconPath = "/icons/TicTacToe.png";
                category = "Puzzle";
                break;
            default:
                bgColor = new Color(0, 123, 255);
                iconPath = "/icons/TicTacToe.png";
                category = "Board";
                break;
        }

        return new GameData(gameName, bgColor, iconPath, category);
    }

    private void displayGames(List<GameData> games) {
        gameGrid.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int cols = 4;
        int count = 0;

        for (GameData game : games) {
            JPanel card = createGameCard(game);
            gameGrid.add(card, gbc);

            count++;
            gbc.gridx++;
            if (count % cols == 0) {
                gbc.gridx = 0;
                gbc.gridy++;
            }
        }

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;

        gameGrid.revalidate();
        gameGrid.repaint();

    }

    private JPanel createGameCard(GameData game) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(game.bgColor);
        card.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(380, 350));

        // Image area
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBackground(game.bgColor);
        imagePanel.setPreferredSize(new Dimension(380, 300));

        URL iconURL = getClass().getResource(game.iconPath);
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (iconURL != null) {
            ImageIcon originalIcon = new ImageIcon(iconURL);
            Image scaledImage = originalIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledImage));
        } else {
            iconLabel.setText("🎮");
            iconLabel.setForeground(Color.WHITE);
            iconLabel.setFont(new Font("Arial", Font.BOLD, 48));
        }

        imagePanel.add(iconLabel);
        card.add(imagePanel, BorderLayout.CENTER);

        // Bottom info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(game.bgColor.darker());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel nameLabel = new JLabel(game.name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel categoryLabel = new JLabel(game.category);
        categoryLabel.setForeground(new Color(220, 220, 220));
        categoryLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        infoPanel.add(nameLabel, BorderLayout.NORTH);
        infoPanel.add(categoryLabel, BorderLayout.SOUTH);

        card.add(infoPanel, BorderLayout.SOUTH);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            private Color originalColor = game.bgColor;

            @Override
            public void mouseClicked(MouseEvent e) {
                mainPanel.add(createGameModePanel(game.name), "gameMode");
                cardLayout.show(mainPanel, "gameMode");
                headerLabel.setText(game.name);
                headerLabel.setVisible(true);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(originalColor.darker());
                imagePanel.setBackground(originalColor.darker());
                infoPanel.setBackground(originalColor.darker().darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(originalColor);
                imagePanel.setBackground(originalColor);
                infoPanel.setBackground(originalColor.darker());
            }
        });

        return card;
    }

    private void filterGames() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.equals("search games...")) {
            searchText = "";
        }

        String selectedCategory = (String) categoryCombo.getSelectedItem();
        String sortBy = (String) sortCombo.getSelectedItem();

        List<GameData> filtered = new ArrayList<>();

        for (GameData game : allGames) {
            // Filter by search
            if (!searchText.isEmpty() && !game.name.toLowerCase().contains(searchText)) {
                continue;
            }

            // Filter by category
            if (!selectedCategory.equals("All Games") && !game.category.equals(selectedCategory)) {
                continue;
            }

            filtered.add(game);
        }

        if (sortBy.equals("Name")) {
            filtered.sort(Comparator.comparing(g -> g.name));
        } else if (sortBy.equals("Popularity")) {
            filtered.sort(Comparator.comparing(g -> g.name));
        }

        displayGames(filtered);
    }

    // Inner class for game data
    private static class GameData {
        String name;
        Color bgColor;
        String iconPath;
        String category;

        public GameData(String name, Color bgColor, String iconPath, String category) {
            this.name = name;
            this.bgColor = bgColor;
            this.iconPath = iconPath;
            this.category = category;
        }
    }

    private JPanel createGameModePanel(String gameName) {
        // Wrapper panel to enforce top alignment
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(28, 28, 30));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        panel.setBackground(new Color(28, 28, 30));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Choose Your Game Mode", SwingConstants.LEFT);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));

        JPanel modeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        modeButtonsPanel.setBackground(new Color(28, 28, 30));
        modeButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton allModesBtn = createModeButton("All Modes", new Color(0, 123, 255), true);
        JButton pvpBtn = createModeButton("Player vs Player", new Color(60, 60, 62), false);
        JButton aiBtn = createModeButton("AI", new Color(60, 60, 62), false);

        modeButtonsPanel.add(allModesBtn);
        modeButtonsPanel.add(pvpBtn);
        modeButtonsPanel.add(aiBtn);
        panel.add(modeButtonsPanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        cardsPanel.setBackground(new Color(28, 28, 30));
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel aiCard = createGameModeCard("AI", "Play against computer", new Color(25, 118, 210), "🤖", 260, 180);
        JPanel randomCard = createGameModeCard("Random", "Quick match with random player", new Color(123, 31, 162), "🎲", 260, 180);
        JPanel findPlayerCard = createGameModeCard("Find Player", "Challenge a specific player", new Color(0, 150, 136), "🔍", 260, 180);

        cardsPanel.add(aiCard);
        cardsPanel.add(randomCard);
        cardsPanel.add(findPlayerCard);

        panel.add(cardsPanel);
        panel.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);

        wrapper.add(panel, BorderLayout.NORTH);

        allModesBtn.addActionListener(e -> {
            updateModeButtonStates(allModesBtn, pvpBtn, aiBtn);
            aiCard.setVisible(true);
            randomCard.setVisible(true);
            findPlayerCard.setVisible(true);
        });

        pvpBtn.addActionListener(e -> {
            updateModeButtonStates(pvpBtn, allModesBtn, aiBtn);
            aiCard.setVisible(false);
            randomCard.setVisible(true);
            findPlayerCard.setVisible(true);
        });

        aiBtn.addActionListener(e -> {
            updateModeButtonStates(aiBtn, allModesBtn, pvpBtn);
            aiCard.setVisible(true);
            randomCard.setVisible(false);
            findPlayerCard.setVisible(false);
        });

        // Card click actions
        aiCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                startAIMode(gameName);
            }
        });

        randomCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                statusLabel.setText("Searching for a random opponent...");
                statusLabel.setForeground(Color.YELLOW);
                String gameType = gameName;
                MatchHandler.setSelectedGameType(client.getPlayerName(), gameType);
                MatchHandler.setMode(client, MatchHandler.Mode.RANDOM);
                try { client.subscribe(gameType); }
                catch (Exception ex) { ex.printStackTrace(); statusLabel.setText("Error subscribing."); statusLabel.setForeground(Color.RED); }
            }
        });

        findPlayerCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                String opponentName = JOptionPane.showInputDialog(panel, "Enter player name:");
                if (opponentName == null || opponentName.isEmpty()) return;
                try {
                    List<String> players = client.getPlayerList();
                    boolean found = players.stream().anyMatch(p -> p.equalsIgnoreCase(opponentName));
                    if (!found) { JOptionPane.showMessageDialog(panel, "Player not found."); return; }
                    MatchHandler.setSelectedGameType(client.getPlayerName(), gameName);
                    MatchHandler.setMode(client, MatchHandler.Mode.FIND_PLAYER);
                    setCurrentOpponentName(opponentName);
                    client.challenge(opponentName, gameName);
                    statusLabel.setText("Invitation sent to " + opponentName);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        return wrapper;
    }


    private JPanel createGameModeCard(String title, String subtitle, Color bgColor, String icon, int width, int height) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(width, height));
        card.setMaximumSize(new Dimension(width, height));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 45));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));

        JLabel subtitleLabel = new JLabel("<html><center>" + subtitle + "</center></html>");
        subtitleLabel.setForeground(new Color(220, 220, 220));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitleLabel);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(bgColor);
            }
        });

        return card;
    }


    private JButton createModeButton(String text, Color bgColor, boolean selected) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        btn.setPreferredSize(new Dimension(140, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Helper method to update button states when clicked
    private void updateModeButtonStates(JButton selected, JButton... others) {
        selected.setBackground(new Color(0, 123, 255));
        for (JButton btn : others) {
            btn.setBackground(new Color(60, 60, 62));
        }
    }

    private void startAIMode(String gameName) {
        BoardGame gamePanel;
        switch (gameName.toLowerCase()) {
            case "tic-tac-toe":
                gamePanel = new TicTacToeGame(null, true, true);
                break;
            case "reversi":
            case "othello":
                gamePanel = new ReversiGame(null, true, true);
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
