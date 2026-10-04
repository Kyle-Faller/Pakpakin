package test;
import javax.swing.*;

public class App {
    
    public static void main(String[] args) throws Exception {
        int boardWidth = 360;
        int boardHeight = 640;
        
        JFrame frame =  new JFrame("Pakpakin");
       // frame.setVisible(true);      
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);     
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Pakpakin flappyBird = new Pakpakin();
        frame.add(flappyBird);
        
        flappyBird.requestFocus();
        frame.setVisible(true);
    
    }  
 
}