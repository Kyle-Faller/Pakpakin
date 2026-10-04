import java.util.*;

public class Test {
   public void main(String[] args) {
      Scanner sc = new Scanner(System.in);
      
      int ticket = 250;
      double discount1 =  250 * .50;
      double discount2 =  250 * .25;
      System.out.print("Enter your age: ");
      int age = sc.nextInt();

      if (age < 0) {
         System.out.println("Invalid age. Please enter a positive number.");    

      }else if(age >= 0 && age <= 12){
         System.out.println("Your ticket cost: P:" + (ticket - discount1));
      }else if(age >= 13 && age <= 59){
         System.out.println("Your ticket cost: P:" + ticket);

      
      }else{
            System.out.println("Your ticket cost: P:" + (ticket - discount2));
      }
      sc.close();
   }    
}

