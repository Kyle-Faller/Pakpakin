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

public class Medium extends JPanel implements ActionListener, KeyListener {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Medium");
        Medium mediumGame = new Medium();
        frame.add(mediumGame);
        frame.setSize(mediumGame.boardWidth, mediumGame.boardHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    int boardWidth = 360;
    int boardHeight = 640;
    
    
 
    Image backgroundImg;
    
    Image topPipImage;
    Image bottomPipImage;
    
  
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
  
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;    
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
   
    Bird bird;
    int velocityX = -6; 
    int velocityY = 0;
    int gravity = 1;
    int animationFrame = 0;
    int animationSpeed = 8;

    ArrayList<Pipe> pipes;
    Random random = new Random();
    
    Timer gameLoop;
    Timer placePipesTimer;
    boolean started = false;    
    boolean gameOver = false;
    double score = 0;
    
      Clip backgroundMusic;

   
    static final Color ORANGE = new Color(255, 105, 25);
    static final Color GREEN = new Color(160, 208, 120);
    static final Color OUTLINE = new Color(110, 45, 15);


    int winScore = 1000;       
    boolean won = false;
    boolean roundFinished = false;
    IntConsumer onLose;         
    IntConsumer onWin;          
    Timer resultTimer;         
      
   
    Medium() {
        this(null, null);
    }


    public Medium(IntConsumer onLose, IntConsumer onWin) {
        this.onLose = onLose;
        this.onWin = onWin;
        setPreferredSize(new Dimension (boardWidth, boardHeight));
     
        setFocusable(true);
        addKeyListener(this);
        
        
        backgroundImg = new ImageIcon(getClass().getResource("/image/pbg.png")).getImage();
        Image birdImg1 = new ImageIcon( getClass().getResource("/image/birdup.png")).getImage();
        Image birdImg2 = new ImageIcon( getClass().getResource("/image/birdown.png")).getImage();
        topPipImage = new ImageIcon(getClass().getResource("/image/toppipepink.png")).getImage();
        bottomPipImage = new ImageIcon(getClass().getResource("/image/bottompipepink.png")).getImage();
        
        bird = new Bird(birdImg1, birdImg2);
        pipes = new ArrayList<Pipe>();
        
        loadBackgroundMusic("/music/background.wav");
       
    
       
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        
        
        gameLoop = new Timer(1000/60, this); 
        gameLoop.start();
    }

  
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
            backgroundMusic.setFramePosition(0); 
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        }
    }

    private void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }
    public void placePipes() {
      
        int randomPipeY = (int) (pipeY - pipeHeight/4 - Math.random()*(pipeHeight/2));
       int openingsPace = boardHeight / 4 - 20; 
       
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

       g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);
       
    
      Image currentBird;

if ((animationFrame / animationSpeed) % 2 == 0) {
    currentBird = bird.img1;
} else {
    currentBird = bird.img2;
}

g.drawImage(currentBird, bird.x, bird.y, bird.width, bird.height, null);
    
       
      for (int i = 0; i < pipes.size(); i++) {
       Pipe pipe = pipes.get(i);
       g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
      }
      
      
      String scoreText = String.valueOf((int) score);
      drawCentered(g, scoreText, scoreText.length(), 24, 6);

      if (!started) {
        
          if ((animationFrame / 30) % 2 == 0) {
              drawCentered(g, "Press space", "Press ".length(), 430, 4);
              drawCentered(g, "to start", 0, 470, 4);
          }
      }
      else if (gameOver && !won && onLose == null) {
          
          drawCentered(g, "Game Over", "Game ".length(), 200, 5);
          drawCentered(g, "Press space to restart", "Press ".length(), 260, 2);
      }
    }

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
     
        animationFrame++;
        velocityY += gravity;
        bird .y += velocityY;
        bird.y = Math.max(bird.y, 0);
        
        
     
        for(int i =0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;
            
            if(!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score+= 0.5; 
            }
            if(collision(bird, pipe)) {
                gameOver = true;
            }
            
        }
        if (bird.y > boardHeight) {
            gameOver = true;
        }

       
        if (onWin != null && !gameOver && score >= winScore) {
            won = true;
        }
    }
    public boolean collision(Bird a, Pipe b) {

    
       int padding = 10;
    int aX = a.x + padding;

    int aY = a.y + padding;

    int aWidth = a.width - padding * 2;
    
    int aHeight = a.height - padding * 2;

    int bX = b.x + padding;

    int bY = b.y + padding;
   

    int bWidth = b.width - padding * 2;
   
    int bHeight = b.height - padding * 2;
   

   return aX < bX + bWidth &&       
       aX + aWidth > bX &&        
       aY < bY + bHeight &&       
       aY + aHeight > bY;         
    }

     @Override
    public void actionPerformed(ActionEvent e) {
        if (!started) {
            
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

    
    private void finishRound() {
        if (roundFinished) return;
        IntConsumer listener = won ? onWin : onLose;
        if (listener == null) return;             
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
           if (won || roundFinished) return;         
           if (!started) {
               
               started = true;
               bird.y = birdY;
               velocityY = -9;
               playMusic();
               placePipesTimer.start();
               return;
           }

           velocityY = -9;
            if (gameOver) {
                
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