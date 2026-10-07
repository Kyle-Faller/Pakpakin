package menu;

public enum Difficulty {
    EASY, MEDIUM, HARD;

  
    public Difficulty next() {
        return values()[(ordinal() + 1) % values().length];
    }

   
    public Difficulty previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}