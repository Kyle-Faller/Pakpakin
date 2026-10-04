import java.awt.Dimension;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import difficulty.Easy;
import difficulty.Hard;
import difficulty.Medium;
import menu.Difficulty;
import menu.LoadingScreen;
import menu.MainMenu;
import screen.Defeat;
import screen.Victory;
import db.Database;

/**
 * Connects all the screens. Run THIS class to start the game.
 *
 *   Start: Name --> Loading --> Main menu (the typed name is saved in Info)
 *   Main menu --Play--> Easy / Medium / Hard (whatever the arrows say)
 *   Every switch to a game or to Victory/Defeat goes through the loading screen first.
 *   Game ends --> Victory (left = next level) or Defeat (left = retry); right = main menu
 *   Main menu --Profile--> Info --Change Name--> Name --> Info
 *   Info --Menu button / ESC--> Main menu
 *
 * The player's name and highest score (best over all three difficulties) are stored in the MySQL
 * database "pakpakin", table "players" (see Database.java, which uses JDBC.java). They are shown on Info, Victory and Defeat.
 */
public class Main {
    private static final int LEVELS = 3;   // 0 = Easy, 1 = Medium, 2 = Hard

    // Show the loading screen while the next screen is being built? (false = switch instantly)
    private static final boolean LOAD_BEFORE_GAME = true;     // menu / retry / next -> Easy, Medium, Hard
    private static final boolean LOAD_BEFORE_RESULT = true;   // game over -> Victory or Defeat

    private final JFrame frame = new JFrame("Pakpakin");
    private final MainMenu menu = new MainMenu();
    private final Info info = new Info(null);
    private Database.Player player = new Database.Player(-1, "Player", 0);   // the current player (a row of the players table)

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        menu.setPlayListener(this::startGame);
        menu.setProfileListener(this::showInfo);
        info.setOnChangeName(this::showNameScreen);
        info.setOnBack(this::showMenu);

        // The game starts on the Name screen. Its panel sets the window size (360 x 640).
        Name nameScreen = new Name(this::nameEntered);
        nameScreen.setPreferredSize(new Dimension(360, 640));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(nameScreen);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Swaps the screen. The old one is removed, so its timers and music stop by themselves. */
    private void show(JPanel screen) {
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
        screen.requestFocusInWindow();
    }

    /** Name was typed: find or create the player in the database behind the loading screen, then open the menu. */
    private void nameEntered(String name) {
         loadThen(() -> {
        Database.Player oldPlayer = Database.findPlayer(name);

        if (oldPlayer != null) {
            JOptionPane.showMessageDialog(
                frame,
                "Welcome back, " + name + "!\n" +
                "Your highest score is " + oldPlayer.highScore() + ".",
                "Player Found",
                JOptionPane.INFORMATION_MESSAGE
            );

            player = oldPlayer;
        } else {
            player = Database.loginOrCreate(name);

            JOptionPane.showMessageDialog(
                frame,
                "Welcome, " + name + "!\nYour new player has been created.",
                "New Player",
                JOptionPane.INFORMATION_MESSAGE
            );
        }

        syncInfo();
        return menu;
    });
}

    /** Copies the current player's name and highest score into the Info screen. */
    private void syncInfo() {
        info.setPlayerName(player.name());
        info.setHighScore(player.highScore());
    }

    private void showMenu() { show(menu); }
    private void showInfo() { show(info); }

    /** Change Name: the new name is saved in the database, then Info shows it. */
    private void showNameScreen() {
        show(new Name(newName -> loadThen(() -> {
            player = Database.rename(player, newName);
            syncInfo();
            return info;
        })));
    }

    /** Play was clicked: open the game that matches the chosen difficulty. */
    private void startGame(Difficulty d) {
        String name = d.name().toUpperCase();
        startLevel(name.equals("MEDIUM") ? 1 : name.equals("HARD") ? 2 : 0);
    }

    /** Opens a fresh game for the level (0 = Easy, 1 = Medium, 2 = Hard), behind the loading screen. */
    private void startLevel(int level) {
        IntConsumer lose = score -> roundEnded(level, score, false);
        IntConsumer win = score -> roundEnded(level, score, true);
        Supplier<JPanel> build = () -> level == 1 ? new Medium(lose, win)
                                     : level == 2 ? new Hard(lose, win)
                                     : new Easy(lose, win);
        if (LOAD_BEFORE_GAME) loadThen(build);
        else show(build.get());
    }

    /** The round ended: save the score, then show Victory or Defeat with the current and highest score. */
    private void roundEnded(int level, int score, boolean won) {
        Supplier<JPanel> build = () -> {
            player = Database.submitScore(player, score);      // writes to the database if it is a new best
            syncInfo();
            int best = player.highScore();

            if (won) {
                Victory victory = new Victory();
                victory.setScores(score, best);
                victory.setLeftListener(() -> {                   // "next": the next difficulty, or the menu after Hard
                    if (level + 1 < LEVELS) startLevel(level + 1);
                    else showMenu();
                });
                victory.setMenuListener(this::showMenu);
                return victory;
            }
            Defeat defeat = new Defeat();
            defeat.setScores(score, best);
            defeat.setLeftListener(() -> startLevel(level));      // "retry": the same difficulty again
            defeat.setMenuListener(this::showMenu);
            return defeat;
        };
        if (LOAD_BEFORE_RESULT) loadThen(build);
        else show(build.get());
    }

    /**
     * Shows the loading screen while 'build' creates the next screen in the background, then lets the
     * bar fill up to 100% and opens that screen. The loading screen stays up until the screen is ready,
     * so the switch never freezes.
     */
    private void loadThen(Supplier<JPanel> build) {
        JPanel[] next = new JPanel[1];
        LoadingScreen loading = new LoadingScreen(() -> show(next[0] != null ? next[0] : menu));
        loading.setProgress(55);                    // the bar runs to 55% and waits for the screen to be ready
        show(loading);

        new SwingWorker<JPanel, Void>() {
            @Override protected JPanel doInBackground() {
                return build.get();
            }

            @Override protected void done() {
                try {
                    next[0] = get();
                } catch (Exception e) {                     // could not build it: go back to the menu instead
                    e.printStackTrace();
                    next[0] = menu;
                }
                loading.setProgress(100);                   // fill the bar; LoadingScreen then opens next[0]
            }
        }.execute();
    }
}