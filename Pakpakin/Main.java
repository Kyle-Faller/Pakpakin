import java.awt.Dimension;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.swing.JFrame;

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


public class Main {
    private static final int LEVELS = 3;  

    private static final boolean LOAD_BEFORE_GAME = true;     
    private static final boolean LOAD_BEFORE_RESULT = true;   

    private final JFrame frame = new JFrame("Pakpakin");
    private final MainMenu menu = new MainMenu();
    private final Info info = new Info(null);
    private Database.Player player = new Database.Player(-1, "Player", 0);   

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        menu.setPlayListener(this::startGame);
        menu.setProfileListener(this::showInfo);
        info.setOnChangeName(this::showNameScreen);
        info.setOnBack(this::showMenu);

       
        Name nameScreen = new Name(this::nameEntered);
        nameScreen.setPreferredSize(new Dimension(360, 640));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(nameScreen);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

   
    private void show(JPanel screen) {
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
        screen.requestFocusInWindow();
    }

    
    private void nameEntered(String name) {
    loadThen(() -> {
        Database.Player found = Database.findPlayer(name);

        if (found != null) {
            player = found;
        } else {
            player = Database.loginOrCreate(name);
        }

        syncInfo();
        return menu;
    });
}


    private void syncInfo() {
        info.setPlayerName(player.name());
        info.setHighScore(player.highScore());
    }

    private void showMenu() { show(menu); }
    private void showInfo() { show(info); }

   
    private void showNameScreen() {
        show(new Name(newName -> loadThen(() -> {
            player = Database.rename(player, newName);
            syncInfo();
            return info;
        })));
    }

    
    private void startGame(Difficulty d) {
        String name = d.name().toUpperCase();
        startLevel(name.equals("MEDIUM") ? 1 : name.equals("HARD") ? 2 : 0);
    }

    
    private void startLevel(int level) {
        IntConsumer lose = score -> roundEnded(level, score, false);
        IntConsumer win = score -> roundEnded(level, score, true);
        Supplier<JPanel> build = () -> level == 1 ? new Medium(lose, win)
                                     : level == 2 ? new Hard(lose, win)
                                     : new Easy(lose, win);
        if (LOAD_BEFORE_GAME) loadThen(build);
        else show(build.get());
    }

   
    private void roundEnded(int level, int score, boolean won) {
        Supplier<JPanel> build = () -> {
            player = Database.submitScore(player, score);     
            syncInfo();
            int best = player.highScore();

            if (won) {
                Victory victory = new Victory();
                victory.setScores(score, best);
                victory.setLeftListener(() -> {                   
                    if (level + 1 < LEVELS) startLevel(level + 1);
                    else showMenu();
                });
                victory.setMenuListener(this::showMenu);
                return victory;
            }
            Defeat defeat = new Defeat();
            defeat.setScores(score, best);
            defeat.setLeftListener(() -> startLevel(level));     
            defeat.setMenuListener(this::showMenu);
            return defeat;
        };
        if (LOAD_BEFORE_RESULT) loadThen(build);
        else show(build.get());
    }

    
    private void loadThen(Supplier<JPanel> build) {
        JPanel[] next = new JPanel[1];
        LoadingScreen loading = new LoadingScreen(() -> show(next[0] != null ? next[0] : menu));
        loading.setProgress(55);                  
        show(loading);

        new SwingWorker<JPanel, Void>() {
            @Override protected JPanel doInBackground() {
                return build.get();
            }

            @Override protected void done() {
                try {
                    next[0] = get();
                } catch (Exception e) {                     
                    e.printStackTrace();
                    next[0] = menu;
                }
                loading.setProgress(100);                   
            }
        }.execute();
    }
}