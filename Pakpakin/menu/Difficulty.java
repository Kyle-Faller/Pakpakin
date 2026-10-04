package menu;
/** The three difficulty levels, in the order the arrows cycle through them. */
public enum Difficulty {
    EASY, MEDIUM, HARD;

    /** The next difficulty (Hard wraps around to Easy). */
    public Difficulty next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The previous difficulty (Easy wraps around to Hard). */
    public Difficulty previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}