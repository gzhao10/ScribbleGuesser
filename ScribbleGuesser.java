/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package scribbleguesser;

import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;


/**
 *
 * @author gzhao10

ScribbleGuesser is a multiplayer game where players take turns drawing, and
the other plays in the server must guess the drawing for points. Only one
player can draw at a time, and the Scribbler is unable to type in chat
during this period. If all players guess correctly, or the timer runs out,
the round ends.
*/

//IMPORTANT: GAME ONLY STARTS WHEN THE PLAYER COUNT IS MET

public class ScribbleGuesser {
    private static final int port = 5190;
     private final static String dbUrl = "jdbc:mariadb://localhost:3306/JavaHW4";
    private final static String dbUser = "root";
    private final static String dbPassword = "password";
    private static ArrayList<ClientProcessor> users = new ArrayList<>();
    
    private static ArrayList<String> words = new ArrayList<>();
    private static String word;
    private static int playerCount = 3;
    private static boolean gameStarted = false;
    private static int numRounds = 3;
    private static int round = 1;
    private static int playerIndex = 0;
    private static int currDrawer = 0;
    private final static int roundLength = 30;
    private static int count = roundLength;
    
    private static String currWord;
    private static boolean [] guessedWord = new boolean [playerCount];
    private static ArrayList<Integer> guessOrder = new ArrayList<>();
    private static int [] points = new int [playerCount];
    
    public static void main(String[] args) {
        initializeDatabase();
        //File IO to read in word bank
        try {
            File file = new File("words.txt");
            Scanner scanner = new Scanner(file);
            
            while (scanner.hasNextLine()) {
                words.add(scanner.nextLine());
            }
            scanner.close();
        } catch (FileNotFoundException e) {}
        
        
        
        //start server and give each client a player id
        try {
            ServerSocket ss = new ServerSocket(port);
            while (!gameStarted){
                Socket client = ss.accept();
                GUI g = new GUI(playerIndex, playerCount);
                playerIndex++;
                ClientProcessor cp = new ClientProcessor(g, client);
                cp.start();
                g.logUser();
                //The game only starts when the player count is met!!!!
                users.add(cp);
                if (users.size() == playerCount)
                    gameStarted = true;
                
            }
        } catch (IOException ex){}
        
        
       //run game until all players have drawn numRounds times
        while (round <= numRounds * playerCount){
            
            //if a new round has started, pick a new word, clear the board, and display hints
            if (count == roundLength){
                word = pickWord();
                reset();
                displayHint(word);
                guessedWord[currDrawer] = true;
            }
            
            //update timers every second    
            updateTimer(count);
            
            count--;
            
            //at the end of a round give the drawer role to the next player and update score
            if (checkEarlyFinish() || count < 0){
                endRound();
                updateScores();
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {}
            }
            
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {}
        }
        
        System.out.println("Done");
        gameStarted = false;
        declareWinner();
    }
    
    //pick a random word from the word bank
    private static String pickWord(){
        Random random = new Random();
        int randomIndex = random.nextInt(words.size());
        currWord = words.get(randomIndex);
        return currWord;
    }
    
    //reset the drawing page
    private static void reset(){
        for (ClientProcessor user : users){
             user.reset();
        }
    }
    
    private static void displayHint(String word){
        for (int i = 0; i < users.size(); i++){
            users.get(i).displayHint(word, i == currDrawer);
        }
    }
    
    private static void updateTimer(int time){
        for (ClientProcessor user : users){
            user.updateTimer(time);
        }
    }
    
    private static void updateScores(){
        for (ClientProcessor user : users){
            user.updateScores(points);
        }
    }
    
    //draw a point on all boards
    public static void drawPoint(int index, int x, int y){
        if (index == currDrawer){
            for (ClientProcessor user : users) {
                user.drawPoint(x, y);
            }
        }
        
    }
    
    //register the start of a mouse drag
    public static void startLine(int index){
        if (index == currDrawer){
            for (ClientProcessor user : users) {
                user.startLine();
            }
        }
    }
    
    //register the end of a mouse drag
    public static void endLine(int index){
        if (index == currDrawer){
            for (ClientProcessor user : users) {
                user.endLine();
            }
        }
    }
    
    //draw a line
    public static void drawLine(int index, int x, int y){
        if (index == currDrawer){
            for (ClientProcessor user : users) {
                user.drawLine(x, y);
            }
        }
    }
    
    //send a message in the chat
    public static void send(String line){
        for (ClientProcessor user : users) {
            user.displayMessage(line);
        }
    }
    
    //display messages if they are not sent by the drawer, or if the player has not already guessed the correct word
    public static void guess(int playerIndex, String name, String line){
        if (playerIndex == currDrawer && gameStarted){
            users.get(playerIndex).displayMessage("You are the scribbler. You cannot guess." + '\n');
        }
        else if (playerIndex != currDrawer && !guessedWord[playerIndex]){
            if (line.toLowerCase().equals(currWord)){
                guessedWord[playerIndex] = true;
                guessOrder.add(playerIndex);
                send(name + " has guessed the word correctly." + '\n');
            }
            else{
                send(name + ": " + line + "\n");
                check(playerIndex, line);
            }
        }
        else{
            send(name + ": " + line + "\n");
            check(playerIndex, line);
        }
    }
    
    private static void check(int playerIndex, String line){
        users.get(playerIndex).check(line);
    }
    
    //take care of end of round protocols
    private static void endRound(){
        send("The word was " + currWord + "\n");
        //score calculations
        for (int i = 0; i < guessOrder.size(); i++){
            int player = guessOrder.get(i);
            points[player] += 500 / (i+1);
        }
        points[currDrawer] += 250 * guessOrder.size();
        
        //reset status of correct guesses to false
        for (int i = 0; i < guessedWord.length; i++){
            guessedWord[i] = false;
        }
       
        guessOrder.clear();
        count = roundLength;
        round++;
        currDrawer++;
        if (currDrawer >= playerCount)
            currDrawer = 0;
        
    }
    
    //check if every non-drawer has guessed the word
    private static boolean checkEarlyFinish(){
        for (boolean status : guessedWord){
            if (!status)
                return false;
        }
        return true;
    }
    
    //declare a winner
    private static void declareWinner(){
        int max = 0;
        for (int i = 0; i < points.length; i++){
            if (points[i] > max){
                max = points[i];
            }
        }
        for (int i = 0; i < users.size(); i++){
            if (points[i] == max)
                users.get(i).displayHint("You Win!", true);
            else{
                users.get(i).displayHint("You Lose!", true);
            }
        }
    }
    
    
    
    private static void initializeDatabase(){
        try{
            Connection conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
            Statement s = conn.createStatement();
            String creation = "create table if not exists LOGINS(ip varchar(16), time timestamp, username varchar(50))";
            s.execute(creation);
        } catch (SQLException ex) {
            System.out.println("Exception: "+ex.toString());
        }
    }
    
    public static void addEntry( Timestamp t, String username){
        try{
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            String query = "insert into LOGINS values(?, ?)";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setTimestamp(1, t); 
            pstmt.setString(2, username); // Assuming username is a variable containing the client's username

            pstmt.execute();
            
        } catch (SQLException ex) {
            System.out.println("Exception: "+ex.toString());
        }
    }
}





class ClientProcessor extends Thread{
    private final Socket client;
    private final GUI g;
    private String username;
    private String message;

    ClientProcessor(GUI newg, Socket newClient){
        g = newg;
        client = newClient;
    }
    
    @Override
    public void run() {
        try {
            boolean cont = true;
            while (cont) {
                if (message != null && message.equalsIgnoreCase("EXIT")){
                    cont = false;
                }
            }
            client.close();
        }
        catch(IOException ex) {}
    }
    
    
    //since g is private, the server must call these methods to make changes to guis
    public void reset(){
        g.reset();
    }
    
    public void displayHint(String word, boolean isDrawer){
        g.displayHint(word, isDrawer);
    }
    
    public void updateTimer(int time){
        g.updateTimer(time);
    }
    
    public void updateScores(int [] scores){
        g.updateScores(scores);
    }
    
    public void drawPoint(int x, int y){
        g.drawPoint(x,y);
    }
    
    public void startLine(){
        g.startLine();
    }
    
    public void endLine(){
        g.endLine();
    }
    
    public void drawLine(int x, int y){
        g.drawLine(x,y);
    }
    
    public void displayMessage(String line){
        g.displayMessage(line);
    }
    
    public void check(String line){
        message = line;
    }

}