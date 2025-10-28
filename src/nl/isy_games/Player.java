package nl.isy_games;

public class Player {
    private final String name;
    private final String mark; // X of O

    public Player(String name, String mark) {
        this.name = name;
        this.mark = mark;
    }

    public String getName() {
        return name;
    }

    public String getMark() {
        return mark;
    }
}