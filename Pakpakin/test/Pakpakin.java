package test;

import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;

public class Pakpakin extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 560;
    int boardHeight = 840;
    
    
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
    

    Bird(Image img1) {
        this.img1 = img1;
        
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
    int velocityX = -3; //moves pipes to the left speed simulates bird moving
    int velocityY = 0;  //move bird up/down speed
    int gravity = 1;


    ArrayList<Pipe> pipes;
    Random random = new Random();
    
    Timer gameLoop;
    Timer placePipesTimer;
    boolean gameOver = false;
    double score = 0;
    
      Clip backgroundMusic;
      
    Pakpakin( ) {
        setPreferredSize(new Dimension (boardWidth, boardHeight));
//        setBackground(Color.blue);
        setFocusable(true);
        addKeyListener(this);
        
        //load images
        backgroundImg = new ImageIcon(getClass().getResource("/image/flappybirdbg2.png")).getImage();
        Image birdImg1 = new ImageIcon( getClass().getResource("/image/birdflying.gif")).getImage();
        
        topPipImage = new ImageIcon(getClass().getResource("/image/pipe2.png")).getImage();
        bottomPipImage = new ImageIcon(getClass().getResource("/image/pipe1.png")).getImage();
        
        bird = new Bird(birdImg1);
        pipes = new ArrayList<Pipe>();
        
        loadBackgroundMusic("/music/background.wav");
    playMusic();
    
        //place pipes timer
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipesTimer.start();
        
        //game time
        gameLoop = new Timer(1000/60, this); //1000/60 = 16.6 frame per second 
        gameLoop.start();
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
     g.drawImage(bird.img1, bird.x, bird.y, bird.width, bird.height, null);
    
       //pipes
      for (int i = 0; i < pipes.size(); i++) {
       Pipe pipe = pipes.get(i);
       g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
      }
      
      //score
      g.setColor(Color.white);
      g.setFont(new Font("Arial", Font.PLAIN, 32));
      
      if (gameOver) {
          g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
      }
      else {
          g.drawString(String.valueOf((int) score), 10, 35);
}
    }
    public void  move() {
        //bird
        
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
        move();
        repaint();
        if(gameOver) {
            placePipesTimer.stop();
            gameLoop.stop();
            stopMusic();
        }
    }
     @Override
    public void keyTyped(KeyEvent e) {
    
    }

    @Override
    public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_SPACE) {
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
