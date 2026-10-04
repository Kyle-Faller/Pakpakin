package difficulty; 

import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.sound.sampled.*;
import ui.Pixelfont;

public class Hard extends JPanel implements ActionListener, KeyListener {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Hard");
        Hard hardGame = new Hard();
        frame.add(hardGame);
        frame.setSize(hardGame.boardWidth, hardGame.boardHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    int boardWidth = 360;
    int boardHeight = 640;
    
    
    //Picture
    Image backgroundImg;
    
    Image topPipImage;
    Image bottomPipImage;
    
    //Langgam
    int birdX = boardWidth/8;
    int birdY = boardHeight/2;
    int birdWidth = 100;
    int birdHeight = 70;

   

    
    
    class Bird {
     int x = birdX;
    int y = birdY;
    int width = birdWidth;
    int height = birdHeight;

    Image img1;
    Image img2;

    Bird(Image img1, Image img2) {
        this.img1 = img1;
        this.img2 = img2;
    }
    }
    //Pipes
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;    //scaled by 1/6
    int pipeHeight = 512;
    
    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;
        
        Pipe(Image img) {
            this.img = img;
            
        }

        public Pipe() {
        }
        
    }
    //game logic
    Bird bird;
    int velocityX = -9; //moves pipes to the left speed simulates bird moving
    int velocityY = 0;  //move bird up/down speed
    int gravity = 1;
    int animationFrame = 0;
    int animationSpeed = 8;

    ArrayList<Pipe> pipes;
    Random random = new Random();
    
    Timer gameLoop;
    Timer placePipesTimer;
    boolean started = false;    // false = waiting on the "Press space to start" screen
    boolean gameOver = false;
    double score = 0;
    
      Clip backgroundMusic;

    //---- Pixel-font colours (same style as the Info screen) ----
    static final Color ORANGE = new Color(255, 105, 25);
    static final Color GREEN = new Color(160, 208, 120);
    static final Color OUTLINE = new Color(110, 45, 15);

    //---- Connection to the menu and the Victory / Defeat screens ----
    int winScore = 2000;        // reach this score to win -> the Victory screen (only when started from the menu)
    boolean won = false;
    boolean roundFinished = false;
    IntConsumer onLose;         // called with the final score after the player crashes (null = play on your own)
    IntConsumer onWin;          // called with the final score after the player reaches winScore
    Timer resultTimer;          // short pause so the player sees what happened before the next screen
      
    /** Stand-alone game (no menu): crash, then press SPACE to restart, like before. */
    Hard() {
        this(null, null);
    }

    /**
     * Game started from the menu.
     * @param onLose  runs with the final score when the bird crashes
     * @param onWin   runs with the final score when the player reaches winScore
     */
    public Hard(IntConsumer onLose, IntConsumer onWin) {
        this.onLose = onLose;
        this.onWin = onWin;
        setPreferredSize(new Dimension (boardWidth, boardHeight));
//        setBackground(Color.blue);
        setFocusable(true);
        addKeyListener(this);
        
        //load images
        backgroundImg = new ImageIcon(getClass().getResource("/image/rbg.png")).getImage();
        Image birdImg1 = new ImageIcon( getClass().getResource("/image/birdup1.png")).getImage();
        Image birdImg2 = new ImageIcon( getClass().getResource("/image/birdown1.png")).getImage();
        topPipImage = new ImageIcon(getClass().getResource("/image/toppipered.png")).getImage();
        bottomPipImage = new ImageIcon(getClass().getResource("/image/bottompipered.png")).getImage();
        
        bird = new Bird(birdImg1, birdImg2);
        pipes = new ArrayList<Pipe>();
        
        loadBackgroundMusic("/music/background.wav");
        // the music and the pipes only begin once the player presses SPACE (see keyPressed)
    
        //place pipes timer (started on the first SPACE)
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        
        //game time - runs from the start so the bird can flap while waiting for SPACE
        gameLoop = new Timer(1000/60, this); //1000/60 = 16.6 frame per second 
        gameLoop.start();
    }

    // The panel needs keyboard focus when it is shown, and must clean up when it is removed
    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    public void removeNotify() {
        placePipesTimer.stop();
        gameLoop.stop();
        if (resultTimer != null) resultTimer.stop();
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.close();
        }
        super.removeNotify();
    }

     private void loadBackgroundMusic(String audioPath) {
        try {
            InputStream is = getClass().getResourceAsStream(audioPath);
            if (is == null) {
                System.out.println("Audio file not found at: " + audioPath);
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(is);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
        } catch (Exception e) {
            System.out.println("Error loading audio: " + e.getMessage());
        }
    }

    private void playMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.setFramePosition(0); // Reset track to beginning
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // Loop continuously
            backgroundMusic.start();
        }
    }

    private void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }
    public void placePipes() {
        //(0-1) * pipeHeight/2 -> (0-256)
        //128
        //0 - 128 (0-256) --> pipeHeight/4 -> 3/4 pipeHeight
        int randomPipeY = (int) (pipeY - pipeHeight/4 - Math.random()*(pipeHeight/2));
        int openingsPace = boardHeight/4;
        //0 - 128 (0-256) --> pipeHeight/4 -> 3/4 pipeHeight
        Pipe topPipe = new Pipe(topPipImage);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);
        
        Pipe bottomPipe = new Pipe(bottomPipImage);
        bottomPipe.y = topPipe.y + pipeHeight + openingsPace;
        pipes.add(bottomPipe);
    }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    private void draw(Graphics g) {
//        System.out.println("draw");
       g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);
       
       //bird
      Image currentBird;

if ((animationFrame / animationSpeed) % 2 == 0) {
    currentBird = bird.img1;
} else {
    currentBird = bird.img2;
}

g.drawImage(currentBird, bird.x, bird.y, bird.width, bird.height, null);
    
       //pipes
      for (int i = 0; i < pipes.size(); i++) {
       Pipe pipe = pipes.get(i);
       g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
      }
      
      //score: top centre, pixel font
      String scoreText = String.valueOf((int) score);
      drawCentered(g, scoreText, scoreText.length(), 24, 6);

      if (!started) {
          // "Press space to start" - blinks so it is easy to notice
          if ((animationFrame / 30) % 2 == 0) {
              drawCentered(g, "Press space", "Press ".length(), 430, 4);
              drawCentered(g, "to start", 0, 470, 4);
          }
      }
      else if (gameOver && !won && onLose == null) {
          // stand-alone game only: from the menu, the Defeat screen takes over instead of this text
          drawCentered(g, "Game Over", "Game ".length(), 200, 5);
          drawCentered(g, "Press space to restart", "Press ".length(), 260, 2);
      }
    }

    /**
     * Pixel-font text centred horizontally with a drop shadow. The characters before 'split' are orange
     * and the rest green (use split = text.length() for all orange, 0 for all green).
     */
    private void drawCentered(Graphics g0, String text, int split, int y, int ps) {
        Graphics2D g = (Graphics2D) g0;
        int x = (boardWidth - Pixelfont.width(text, ps)) / 2;
        int shadow = Math.max(1, ps / 2);
        split = Math.max(0, Math.min(split, text.length()));
        Pixelfont.drawShadowed(g, text.substring(0, split), x, y, ps, ORANGE, OUTLINE, shadow);
        if (split < text.length()) {
            Pixelfont.drawShadowed(g, text.substring(split), x + split * 6 * ps, y, ps, GREEN, OUTLINE, shadow);
        }
    }

    public void  move() {
        //bird
        animationFrame++;
        velocityY += gravity;
        bird .y += velocityY;
        bird.y = Math.max(bird.y, 0); //limits the bird to move up
        
        
        //pipes
        for(int i =0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;
            
            if(!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score+= 0.5; //0.5 because theres 2 pipes and if you combine them its gonna be 1
            }
            if(collision(bird, pipe)) {
                gameOver = true;
            }
            
        }
        if (bird.y > boardHeight) {
            gameOver = true;
        }

        // Reached the winning score (only when the game was started from the menu)
        if (onWin != null && !gameOver && score >= winScore) {
            won = true;
        }
    }
    public boolean collision(Bird a, Pipe b) {
    //detecting collision
    
       int padding = 10; // Gipa-gamay ang hitbox
    int aX = a.x + padding;
// I-move ang X position pasulod og 10 pixels
    int aY = a.y + padding;
  // I-move ang Y position pasulod og 10 pixel
    int aWidth = a.width - padding * 2;
    // Gipa-gamay ang width kay kuhaan og 10 pixels sa left ug right
    int aHeight = a.height - padding * 2;
// Gipa-gamay ang height kay kuhaan og 10 pixels sa taas ug ubos
    int bX = b.x + padding;
    // I-move ang X position pasulod og 10 pixels
    int bY = b.y + padding;
    // I-move ang Y position pasulod og 10 pixels

    int bWidth = b.width - padding * 2;
    // Gipa-gamay ang width kay kuhaan og 10 pixels sa left ug right

    int bHeight = b.height - padding * 2;
    // Gipa-gamay ang height kay kuhaan og 10 pixels sa taas ug uboss

   return aX < bX + bWidth &&        // I-check kung ang left side sa bird naa pa sa range sa right side sa pipe
       aX + aWidth > bX &&        // I-check kung ang right side sa bird niabot/nisulod sa left side sa pipe
       aY < bY + bHeight &&       // I-check kung ang top sa bird naa pa sa range sa bottom sa pipe
       aY + aHeight > bY;         // I-check kung ang bottom sa bird niabot/nisulod sa top sa pipe
    }

     @Override
    public void actionPerformed(ActionEvent e) {
        if (!started) {
            // waiting for SPACE: the bird flaps and floats up and down, nothing else moves
            animationFrame++;
            bird.y = birdY + (int) (Math.sin(animationFrame * 0.12) * 8);
            repaint();
            return;
        }
        move();
        repaint();
        if(gameOver || won) {
            placePipesTimer.stop();
            gameLoop.stop();
            stopMusic();
            finishRound();
        }
    }

    /** After a short pause, tells the menu code how the round ended so it can show Victory or Defeat. */
    private void finishRound() {
        if (roundFinished) return;
        IntConsumer listener = won ? onWin : onLose;
        if (listener == null) return;                 // stand-alone game: stay here, SPACE restarts
        roundFinished = true;
        final int finalScore = (int) score;
        resultTimer = new Timer(800, e -> listener.accept(finalScore));
        resultTimer.setRepeats(false);
        resultTimer.start();
    }
     @Override
    public void keyTyped(KeyEvent e) {
    
    }

    @Override
    public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_SPACE) {
           if (won || roundFinished) return;          // the Victory / Defeat screen takes over

           if (!started) {
               // first SPACE: the game really begins now
               started = true;
               bird.y = birdY;
               velocityY = -9;
               playMusic();
               placePipesTimer.start();
               return;
           }

           velocityY = -9;
            if (gameOver) {
                //restart the game
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                score = 0;
                gameOver = false;
                 playMusic();
                gameLoop.start();
                placePipesTimer.start();
            }
       }       
    }

    @Override
    public void keyReleased(KeyEvent e) {
        
    }
}