/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/File.java to edit this template
 */
package scribbleguesser;

/**
 *
 * @author gzhao10
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import java.sql.*;


public class GUI {
    
    public static void main(String args[]) {
        try{
            Socket client = new Socket("localhost", 5190);
        } catch (IOException ex){}
    }
    
    static Color activeColor;
    static int thickness;
    private final String name;
    private final JLabel hint = new JLabel();
    private final JLabel time = new JLabel();
    
    private final Chatbox chat;
    private final Display d;
    private final Scoreboard sb;
    
    private final int playerIndex;
    private static int playerCount;
    
    
    public GUI(int newPlayerIndex, int newPlayerCount) {
       
        playerIndex = newPlayerIndex;
        playerCount = newPlayerCount;
        
        name = "Player " + Integer.toString(playerIndex + 1);
        activeColor = Color.BLACK;
        thickness = 10;
        JFrame jf = new JFrame("ScribbleGuesser");
        jf.setSize(1200,600);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        d = new Display();
        PressListener pl = new PressListener(d, playerIndex);
        d.addMouseListener(pl);
        d.addMouseMotionListener(pl);
        
        ButtonPanel butts = new ButtonPanel();
        chat = new Chatbox(playerIndex, name);
        sb = new Scoreboard(playerIndex, playerCount);
        
        JPanel background = new JPanel(new BorderLayout());
        jf.add(background);
        
        JPanel topBar = new JPanel(new BorderLayout());
        background.add(topBar, BorderLayout.NORTH);

        hint.setFont(new Font("Arial", Font.BOLD, 24));
        hint.setHorizontalAlignment(JLabel.CENTER);
        time.setFont(new Font("Arial", Font.BOLD, 24));
        topBar.setBackground(new Color(232,232,232));
        topBar.add(time, BorderLayout.WEST);
        topBar.add(hint, BorderLayout.CENTER);
        
        JPanel split = new JPanel();
        split.setLayout(new GridLayout(1,2));
        background.add(split, BorderLayout.CENTER);
        
        
        JPanel drawingSide = new JPanel(new BorderLayout());
        drawingSide.add(d, BorderLayout.CENTER);
        drawingSide.add(butts, BorderLayout.SOUTH);
        
        JPanel rightSide = new JPanel();
        rightSide.setLayout(new GridLayout(2,1));
        rightSide.add(sb);
        rightSide.add(chat);
        
        
        split.add(drawingSide);
        split.add(rightSide);
        
        jf.setVisible(true);
        
    }
    
    public void displayHint(String word, boolean isDrawer){
        String ans = "";
        if (!isDrawer){
            for (int i = 0; i < word.length(); i++){
                ans += "_";
                if (i != word.length()-1){
                    ans += " ";
                }
            }
            hint.setText(ans);
        }
        else{
            hint.setText(word);
        }
    }
    
    public void updateTimer(int count){
        String ans = Integer.toString(count);
        time.setText(ans);
    }
    
    //display guess on this GUI
    public void displayMessage(String message){
        chat.displayMessage(message + "\n");
    }
    
    //draw point
    public void drawPoint(int x, int y) {
         d.addPoint(x, y);
         d.repaint();
    }
    
    //start line
    public void startLine(){
        d.lines.add(d.currentLine);
    }
    
    //end line
    public void endLine(){
        d.lines.add(new ArrayList<>(d.currentLine));
        d.currentLine.clear();
    }
    
    //draw line
    public void drawLine(int x, int y){
        d.addLinePoint(x, y);
        d.repaint();
    }
    
    //reset display
    public void reset(){
        d.points.clear();
        d.lines.clear();
        d.currentLine.clear();
        d.repaint();
    }
    
    public void updateScores(int[] points){
        sb.updateScores(points);
    }
    
    
    public void logUser(){
        ScribbleGuesser.addEntry(new Timestamp(System.currentTimeMillis()), name);
    }
}

//panel for players to draw on
class Display extends JPanel{
    class Location{
        int x;
        int y;
        Color c;
        int thick;
        Location(int newx, int newy, Color newc, int newthick){
            x = newx;
            y = newy;
            c = newc;
            thick = newthick;
        }
    }
    
    static ArrayList<Location> points = new ArrayList<>();
    ArrayList<ArrayList<Location>> lines = new ArrayList<>();
    ArrayList<Location> currentLine = new ArrayList<>();
    
    public Display(){
        super();
        setBackground(Color.WHITE);
    }
    
    public void addPoint(int x, int y){
        points.add(new Location(x,y,GUI.activeColor, GUI.thickness));
    }
    public void addLinePoint(int x, int y){
        currentLine.add(new Location(x,y,GUI.activeColor, GUI.thickness));
    }
  
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        
        //click mouse
        for (Location l: points){
            g.setColor(l.c);
            g.fillRect(l.x-5, l.y-5, l.thick, l.thick);
        }
        
        //drag mouse
        for (ArrayList<Location> line : lines) {
            if (!line.isEmpty()){
                Location prev = line.get(0);
                g.setColor(prev.c);
                for (int i = 1; i < line.size(); i++) {
                    Location next = line.get(i);
                    
                    //line thickness
                    for (int j = 0; j < prev.thick; j++) {
                        g.drawLine(prev.x + j, prev.y + j, 
                                   next.x + j, next.y + j);
                    }
                    prev = next;
                }
            }
        }
        
    }
}

//panel for buttons to choose colors and brush sizes
class ButtonPanel extends JPanel{
    static JButton[] buttons = new JButton[9];
    
    public ButtonPanel(){
        super();
        createButtons();
    }
    
    private void createButtons(){
        JPanel colors = new JPanel();
        colors.setLayout(new GridLayout(2,3));
        JPanel brushes = new JPanel();
        brushes.setLayout(new GridLayout(1,3));
        add(colors, BorderLayout.WEST);
        add(brushes, BorderLayout.EAST);
        
        Color [] buttonColors = new Color[] {Color.GRAY, Color.RED, Color.BLUE, 
                                             Color.GREEN, Color.BLACK, Color.WHITE};
        ButtonListener bl = new ButtonListener();
        for (int i = 0; i < buttons.length; i++){
            if (i < 6){
                buttons[i] = new JButton();
                buttons[i].setBorderPainted(false);
                buttons[i].setOpaque(true);
                buttons[i].setBackground(buttonColors[i]);
                colors.add(buttons[i]);
            }
            else{
                if (i == 6)
                    buttons[i] = new JButton("SMALL");
                else if (i == 7)
                    buttons[i] = new JButton("MEDIUM");
                else{
                    buttons[i] = new JButton("LARGE");
                }
                brushes.add(buttons[i]);
            }
            buttons[i].addActionListener(bl);
        }
    }
}


//panel for the chat box
class Chatbox extends JPanel{
    private JTextArea messages;
    private final int playerIndex;
    private final String name;
    
    public Chatbox(int newPlayerIndex, String newName){
        super();
        playerIndex = newPlayerIndex;
        name = newName;
        setLayout(new BorderLayout());
        createGUI();
    }
    
    private void createGUI(){
        messages = new JTextArea(20,40);
        messages.setBackground(new Color(227, 238, 255));
        messages.setCaretColor(new Color(227, 238, 255));
        messages.setEditable(false);

        JScrollPane display = new JScrollPane(messages);
        
        JPanel input = new JPanel();
        input.setLayout(new BorderLayout());
        JTextField text = new JTextField(20);
        JButton sendButton = new JButton("Send");
        input.add(text);
        input.add(sendButton, BorderLayout.EAST);
        
        add(display, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);
        
        
        sendButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                String line = text.getText().trim();
                text.setText("");
                if (!line.isEmpty()) {
                    ScribbleGuesser.guess(playerIndex, name, line);
                }
            }
        });
    }
    
    public void displayMessage(String message){
        messages.append(message + "\n");
    }
    
}

//panel for scoreboard
class Scoreboard extends JPanel{
    private final int playerIndex;
    private final int playerCount;
    private final ArrayList<JLabel> display = new ArrayList<>();
    
    public Scoreboard(int newPlayerIndex, int newPlayerCount){
        super();
        playerIndex = newPlayerIndex;
        playerCount = newPlayerCount;
        createGUI();
    }
    
    private void createGUI(){
        setLayout(new GridLayout(playerCount,1));
        int [] scores = new int [playerCount];
        
        for (int i = 0; i < playerCount; i++){
            JLabel s = new JLabel();
            s.setFont(new Font("Arial", Font.BOLD, 18));
            if (i == playerIndex)
               s.setForeground(new Color (37, 128, 61));
            display.add(s);
            add(s);
        }
        updateScores(scores);
    }
    
    public void updateScores(int [] scores){
       for (int i = 0; i < playerCount; i++){
           display.get(i).setText("   Player " + Integer.toString(i+1) + ": " + Integer.toString(scores[i]));
       }
    }
    
}

//button
class ButtonListener implements ActionListener{
    @Override
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        if (button.getBackground() == Color.GRAY)
            GUI.activeColor = Color.GRAY;
        else if(button.getBackground() == Color.RED)
            GUI.activeColor = Color.RED;
        else if(button.getBackground() == Color.BLUE)
            GUI.activeColor = Color.BLUE;
        else if(button.getBackground() == Color.GREEN)
            GUI.activeColor = Color.GREEN;
        else if(button.getBackground() == Color.BLACK)
            GUI.activeColor = Color.BLACK;
        else if(button.getBackground() == Color.WHITE)
            GUI.activeColor = Color.WHITE;
        
        else if (button.getText().equals("SMALL"))
            GUI.thickness = 5;
        else if (button.getText().equals("MEDIUM"))
            GUI.thickness = 10;
        else{
            GUI.thickness = 20;
        }
        
    }
    
}

//mouse
class PressListener extends MouseAdapter{
    private final Display d;
    private final int playerIndex;
    PressListener(Display newd, int newPlayerIndex){
        d = newd;
        playerIndex = newPlayerIndex;
    }
    @Override
    public void mouseClicked(MouseEvent e){
        ScribbleGuesser.drawPoint(playerIndex, e.getX(), e.getY());
        
    }
    @Override
    public void mousePressed(MouseEvent e) {
        ScribbleGuesser.startLine(playerIndex);
        
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        ScribbleGuesser.endLine(playerIndex);
        
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        ScribbleGuesser.drawLine(playerIndex, e.getX(), e.getY());
        
    }
}