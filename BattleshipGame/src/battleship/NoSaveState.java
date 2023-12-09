package battleship;

import java.util.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.FontMetrics;

public class NoSaveState extends JFrame implements Runnable {
    private static final Color[] TILE_COLORS = {
            Color.BLUE,   // open
            Color.GRAY,   // ship
            Color.WHITE,  // miss
            Color.RED     // hit
    };
    private static final String[] directions= {
            "up",
            "down",
            "left",
            "right"
    };

    private static final String[] prompts= {
            "First, set your Carrier",
            "Now set your Battleship",
            "Now set your Destroyer",
            "Now set your Submarine",
            "Lastly, set your Patrol Boat"
    };

    //1=open, 2=ship, 3=miss, 4=hit
    private static final long serialVersionUID = 1L;
    private int gamewidth=1500;
    private int gameheight=600;
    private int gridwidth=gamewidth/4;
    private int gridheight=gridwidth;
    private int cellwidth=gridwidth/10;
    private int cellheight=gridheight/10;

    private int mygrid[][]=new int[10][10];
    private int opgrid[][]=new int[10][10];
    private int xoffset=(gamewidth/3-gridwidth)/2;
    private int yoffset=(gameheight-gridheight)/2;
    private boolean gameinprogress,shipsset=false, playerWon = false, computerWon = false, loadedGame = false;
    private Queue<Coordinate> attack;

    private JLabel warning,enterlabel,userenternamelabel;
    private JTextField enter,userentername;
    private String choice,lastchoice;
    private JTextArea messages, winner;
    private JScrollPane scroll;

    private ArrayList<Ship> myships=new ArrayList<Ship>();
    private ArrayList<Ship> opships=new ArrayList<Ship>();
    private ImagePanel leftboard;
    private ImagePanel rightboard;
    private String username="Jacinto";
    private String password="1234";
    private int ophitsleft;
    private int myhitsleft;

    private Socket socket;
    ObjectOutputStream toServer = null;
    ObjectInputStream fromServer = null;

    public NoSaveState(String username,String password) {
        this.username=username;
        this.password=password;
        for(int x=0;x<10;x++) {
            for(int y=0;y<10;y++) {
                mygrid[x][y]=1;
                opgrid[x][y]=1;
            }
        }
        ophitsleft=myhitsleft=17;
        createships();
        attack=new LinkedList<Coordinate>();
        setopponentships();
        //setuserships();
        //randomizeuserships();
        launchgame();
    }

    public int[][] getMyGrid(){
        return this.mygrid;
    }

    public int[][] getOpGrid(){
        return this.opgrid;
    }

    public Queue<Coordinate> getAttack(){
        return this.attack;
    }

    public ArrayList<Ship> getMyships(){
        return this.myships;
    }

    public ArrayList<Ship> getOpships(){
        return this.opships;
    }

    public int getMyhitsleft() {
        return this.myhitsleft;
    }

    public int getOphitsleft() {
        return this.ophitsleft;
    }

    public boolean getShipsset() {
        return this.shipsset;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public long getSerialId() {
        return this.serialVersionUID;
    }

    public void setwarning(String warn) {
        this.warning.setText(warn);
    }

    public void setenterlabel(String text){
        this.enterlabel.setText(text);
    }

    public void launchgame() {
        gameinprogress=true;
        //attack=new LinkedList<Coordinate>();
        enter=new JTextField(5);
        enter.addActionListener(new textfieldlistener());
        this.setBackground(Color.LIGHT_GRAY);
        setSize(gamewidth,gameheight);
        createpanel();
        Thread t=new Thread(this);
        t.start();
    }

    public void run() {
        if(shipsset == false) {
            setenterlabel("Set your Carrier - 5 spaces (ship 1/5)");
        }
        else {
            setenterlabel("Enter coordinates to attack:");
        }
        while(gameinprogress) {
            while(choice==null) {
                timedelay(0.25);
            }
            if(shipsset) {
                myturn();
                leftboard.repaint();
                rightboard.repaint();
                if (gameinprogress) {
                    computerturn();
                    leftboard.repaint();
                    rightboard.repaint();
                }
            }else {
                int q=1;
                for(Ship s:myships) {
                    //setenterlabel("Ship "+q+"/5 - Set your "+s.name+" ("+s.holes+" spaces)");
                    setenterlabel("Set your "+s.name+" - "+s.holes+" spaces (ship "+q+"/5)");
                    q++;
                    while(choice==null||choice==lastchoice) {
                        timedelay(0.25);
                    }
                    setusership(s);
                    leftboard.repaint();
                    rightboard.repaint();
                    lastchoice=choice;
                }
                shipsset=true;
                setenterlabel("Enter coordinates to attack:");
            }
            choice=null;
        }
    }

    private class ImagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);
            g.drawRect(xoffset-1, yoffset-1, gridwidth+2, gridheight+2);
        }
    }

    public void createpanel() {
        rightboard=new ImagePanel(){
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = cellwidth;
                int height = cellheight;
                g.setColor(Color.BLACK);
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        g.drawRect((x * width)+xoffset, (y * height)+yoffset, width, height);
                    }
                }
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        char corner=isthisacornerm(x,y);
                        g.setColor(gettilecolor(mygrid[x][y],true));
                        if(corner=='n') {
                            g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2);
                        }else {
                            g.setColor(Color.BLUE);
                            g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2);
                            g.setColor(Color.GRAY);
                            if(corner=='l') {
                                g.fillRect((x * width)+xoffset+cellwidth/2+1, (y * height)+yoffset+1, width/2-1, height-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='r') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width/2-2, height-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='t') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+cellwidth/2+1, width-2, height/2-1);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='b') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height/2-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                        }
                    }
                }
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        g.setColor(getholecolor(mygrid[x][y],true));
                        g.fillOval((x * width)+xoffset+cellwidth/4, (y * height)+yoffset+cellwidth/4, width/2, height/2);
                    }
                }
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                for (int y = 0; y < 10; y++) {
                    g.setColor(Color.BLACK);
                    g.drawString(String.valueOf(convert(y)), xoffset-25, (y*cellheight+yoffset+cellheight/2)+5);
                }
                for(int x=0;x<10;x++) {
                    g.drawString(String.valueOf(x+1),(xoffset+10)+x*cellwidth,yoffset-5);
                }
                FontMetrics fm = g.getFontMetrics();
                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString(username,xoffset + (gridwidth - fm.stringWidth(username)) / 2,yoffset-50);
            }
        };
        leftboard=new ImagePanel(){
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = cellwidth;
                int height = cellheight;
                g.setColor(Color.BLACK);
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        g.drawRect((x * width)+xoffset, (y * height)+yoffset, width, height);
                    }
                }
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        char corner=isthisacornero(x,y);
                        g.setColor(gettilecolor(opgrid[x][y],false));
                        if(corner=='n') {
                            g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2);
                        }else {
                            g.setColor(Color.BLUE);
                            g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2);
                            g.setColor(gettilecolor(opgrid[x][y],false));
                            if(corner=='l') {
                                g.fillRect((x * width)+xoffset+cellwidth/2+1, (y * height)+yoffset+1, width/2-1, height-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='r') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width/2-2, height-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='t') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+cellwidth/2+1, width-2, height/2-1);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                            if(corner=='b') {
                                g.fillRect((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height/2-2);
                                g.fillArc((x * width)+xoffset+1, (y * height)+yoffset+1, width-2, height-2,0,360);
                            }
                        }
                    }
                }
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        g.setColor(getholecolor(opgrid[x][y],false));
                        g.fillOval((x * width)+xoffset+cellwidth/4, (y * height)+yoffset+cellwidth/4, width/2, height/2);
                    }
                }
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                for (int y = 0; y < 10; y++) {
                    g.drawString(String.valueOf(convert(y)), xoffset-25, (y*cellheight+yoffset+cellheight/2)+5);
                }
                for(int x=0;x<10;x++) {
                    g.drawString(String.valueOf(x+1),(xoffset+10)+x*cellwidth,yoffset-5);
                }
                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString("Opponent",xoffset+(gridwidth/3),yoffset-50);
            }
        };
        JPanel mainpan=new JPanel();
        mainpan.setLayout(new GridLayout(1,2));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());


        //rightboard.add(enter,BorderLayout.NORTH);
        //rightboard.add(fire,BorderLayout.NORTH);
        warning=new JLabel("");
        warning.setForeground(Color.RED);
        rightboard.add(warning,BorderLayout.NORTH);
        messages=new JTextArea();
        messages.setPreferredSize(new Dimension(200, messages.getPreferredSize().height));
        messages.setEditable(false);
        messages.setLineWrap(true);
        scroll=new JScrollPane(messages);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(10, 10));
        winner=new JTextArea();
        winner.setEditable(false);
        leftboard.add(messages,BorderLayout.SOUTH);
        mainpan.add(leftboard);
        mainpan.add(messages);
        mainpan.add(rightboard);
        this.add(mainpan,BorderLayout.CENTER);
        rightboard.repaint();
        leftboard.repaint();
        JPanel enterPanel = new JPanel();
        enterPanel.setLayout(new GridLayout(1,2));
        enterlabel = new JLabel("");
        enterPanel.add(enterlabel);
        enterPanel.add(enter);
        topPanel.add(enterPanel,BorderLayout.EAST);
        topPanel.add(winner,BorderLayout.NORTH);
        this.add(topPanel, BorderLayout.NORTH);
        this.add(topPanel,BorderLayout.NORTH);
    }

    public boolean isvalid(int x,int y) {return x>=0&&y>=0&&x<=9&&y<=9;}

    public void myturn() {
        //System.out.println("my choice is "+choice);
        int x=-1,y=-1,counter=0;
        boolean error=false;
        do {
            try {
                if(counter>=1) {
                    if (isvalid(x, y)) {
                        setwarning(choice+" is already guessed! Please guess again");
                        //System.out.println("that coordinate is already guessed; guess again");
                    }else {
                        if(!error) {
                            setwarning(choice + " is not a valid coordinate! Please guess again");
                        }
                        //System.out.println(choice + " is not a valid coordinate! Please guess again");
                    }
                }
                y=reverse(choice.charAt(0));
                x = Integer.parseInt(choice.substring(1))-1;
                counter++;
            }catch(Exception e) {
                setwarning("Invalid coordinate! Please guess again");
                x=y=-1;
                error=true;
                //System.out.println("invalid input");
            }
        }while(!isvalid(x,y)||opgrid[x][y]==3||opgrid[x][y]==4);
        setwarning("");
        String hitormiss="";
        String aftermessage="";
        if(opgrid[x][y]==1){
            hitormiss="Miss";
            opgrid[x][y]=3;
        } //I missed
        if(opgrid[x][y]==2) {
            hitormiss="Hit";
            opgrid[x][y]=4;
            ophitsleft--;
            for(Ship s:opships) {
                s.checkifstruck(new Coordinate(x,y));
            }
            for(Ship s:opships) {
                if(s.active&&(s.holes==s.struck)) {
                    s.active=false;
                    aftermessage=username+" sunk the computer's "+s.name+'\n';
                }
            }
            checkifgameover();
        } //I hit a ship
        messages.insert(this.username+" guesses "+choice+" - "+hitormiss+'\n',0);
        //System.out.println("Myturn grid: \n");
        //printGrid();
        if(aftermessage!="") {
            timedelay(0.25);
            messages.insert(aftermessage,0);
        }
    }

    public void computerturn() {
        timedelay(1);
        int x=-1;
        int y=-1;
        String hitormiss="";
        String computerguess="";
        String aftermessage="";
        if(!attack.isEmpty()) {
            do {
                if(attack.isEmpty()){break;}
                Coordinate suggestion = attack.remove();
                x = suggestion.rawcolumn;
                y = suggestion.rawrow;
            }while(alreadyhitorsurrounded(x,y));
        }else {
            do {
                x=pickspot(10);
                y=pickspot(10);
            }while(alreadyhitorsurrounded(x,y));
        }
        computerguess+=convert(y)+Integer.toString(x+1);
        if(mygrid[x][y]==1) {
            hitormiss="Miss";
            mygrid[x][y]=3;
        } //computer missed
        if(mygrid[x][y]==2) {
            hitormiss="Hit";
            mygrid[x][y]=4;
            myhitsleft--;
            List<Integer> order=Arrays.asList(1,2,3,4);
            Collections.shuffle(order);
            for(int z=0;z<4;z++) {
                addsuspiciouslocations(x,y,order.get(z));
            }
            for(Ship s:myships) {
                s.checkifstruck(new Coordinate(x,y));
            }
            for(Ship s:myships) {
                if(s.active&&(s.holes==s.struck)) {
                    s.active=false;
                    aftermessage="Computer sunk "+this.username+"'s "+s.name+'\n';
                    //attack.clear();
                }
            }
            checkifgameover();
        } //my ship has been hit
        messages.insert("Computer guesses "+computerguess+" - "+hitormiss+'\n',0);
        if(aftermessage!="") {
            timedelay(0.25);
            messages.insert(aftermessage,0);
        }
    }

    public void addsuspiciouslocations(int x,int y,int c) {
        try{
            if(c==1&&isvalid(x,y-1)&&mygrid[x][y-1]!=3&&mygrid[x][y-1]!=4) {attack.add(new Coordinate(x,y-1));};
            if(c==2&&isvalid(x,y+1)&&mygrid[x][y+1]!=3&&mygrid[x][y+1]!=4) {attack.add(new Coordinate(x,y+1));};
            if(c==3&&isvalid(x+1,y)&&mygrid[x+1][y]!=3&&mygrid[x+1][y]!=4) {attack.add(new Coordinate(x+1,y));};
            if(c==4&&isvalid(x-1,y)&&mygrid[x-1][y]!=3&&mygrid[x-1][y]!=4) {attack.add(new Coordinate(x-1,y));};
        }catch(Exception e) {
            //System.out.println("Index was out of bounds");
            System.out.println(" ");
        }
    }

    public boolean alreadyhitorsurrounded(int x,int y) {
        boolean bottom=false,top=false,left=false,right=false;
        if (mygrid[x][y] == 3 || mygrid[x][y] == 4) {return true;}
        try{
            bottom=mygrid[x][y-1]==3;
        }catch(Exception e){
            bottom=true;
        }
        try{
            top=mygrid[x][y+1]==3;
        }catch(Exception e){
            bottom=true;
        }
        try{
            left=mygrid[x-1][y]==3;
        }catch(Exception e){
            bottom=true;
        }
        try{
            right=mygrid[x+1][y]==3;
        }catch(Exception e){
            bottom=true;
        }
        return bottom&&top&&left&&right;
    }

    public void createships() {
        Ship destroyer=new Ship("Destroyer",3);
        Ship submarine=new Ship("Submarine",3);
        Ship patrol=new Ship("Patrol Boat",2);
        Ship battleship=new Ship("Battleship",4);
        Ship carrier=new Ship("Carrier",5);
        Ship enemy_destroyer=new Ship("Destroyer",3);
        Ship enemy_submarine=new Ship("Submarine",3);
        Ship enemy_patrol=new Ship("Patrol Boat",2);
        Ship enemy_battleship=new Ship("Battleship",4);
        Ship enemy_carrier=new Ship("Carrier",5);
        myships.add(carrier);
        myships.add(battleship);
        myships.add(destroyer);
        myships.add(submarine);
        myships.add(patrol);
        opships.add(enemy_destroyer);
        opships.add(enemy_submarine);
        opships.add(enemy_patrol);
        opships.add(enemy_battleship);
        opships.add(enemy_carrier);
    }

    public int pickspot(int rng) {
        Random r=new Random();
        return r.nextInt(rng);
    }

    public boolean conflicts(int col,int row,String dir,int len,boolean my) {
        int grid[][];
        grid=(my)?mygrid:opgrid;
        if(dir=="up") {
            if(len-row-1>0) {return true;}
            while(len>0&&row>=0) {
                if(grid[col][row]==2) {return true;};
                len--;
                row--;
            }
        }
        if(dir=="down") {
            if(len-1+row>9) {return true;}
            while(len>0&&row<=9) {
                if(grid[col][row]==2) {return true;};
                len--;
                row++;
            }
        }
        if(dir=="left") {
            if(len-col-1>0) {return true;}
            while(len>0&&col>=0) {
                if(grid[col][row]==2) {return true;};
                len--;
                col--;
            }
        }
        if(dir=="right") {
            if(len-1+col>9) {return true;}
            while(len>0&&col<=9) {
                if(grid[col][row]==2) {return true;};
                len--;
                col++;
            }
        }
        return false;
    }

    public void randomizeuserships() {
        for(Ship s:myships) {
            int col=pickspot(10);
            int row=pickspot(10);
            String dir=directions[pickspot(4)];
            int len=s.getHoles();
            while(conflicts(col,row,dir,len,true)) {
                col=pickspot(10);
                row=pickspot(10);
                dir=directions[pickspot(4)];
                len=s.getHoles();
            }
            if(dir=="up") {
                while(len>0&&row>=0) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row--;
                }
                s.coords.get(0).special='b';
                s.coords.get(s.holes-1).special='t';
            }
            if(dir=="down") {
                while(len>0&&row<=9) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row++;
                }
                s.coords.get(0).special='t';
                s.coords.get(s.holes-1).special='b';
            }
            if(dir=="left") {
                while(len>0&&col>=0) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col--;
                }
                s.coords.get(0).special='r';
                s.coords.get(s.holes-1).special='l';
            }
            if(dir=="right") {
                while(len>0&&col<=9) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col++;
                }
                s.coords.get(0).special='l';
                s.coords.get(s.holes-1).special='r';
            }
        }
//		for(Ship s:myships) {
//			s.printcoords();
//			System.out.println();
//		}
    }

    public String approval(String d){
        if(d.equals("u")||d.equals("U")){return "up";}
        if(d.equals("d")||d.equals("D")){return "down";}
        if(d.equals("l")||d.equals("L")){return "left";}
        if(d.equals("r")||d.equals("R")){return "right";}
        return "error";
    }

    public void setusership(Ship s) {
        int counter=0;
        int col=-1,row=-1,len=10;
        String dir="uninit";
        do {
            try {
                if (counter == 1) {
                    setwarning("Invalid/overlapping position! Please pick again");
                }
                if(dir.equals("error")) {
                    setwarning("Invalid ship direction! Must be U, D, L or R");
                }
                String[] params = choice.split(" ");
                //System.out.println(params[0] + " and " + params[1] + " and " + choice);
                row = reverse(params[0].charAt(0));
                col = Integer.parseInt(params[0].substring(1)) - 1;
                dir = approval(params[1]);
                len = s.holes;
                counter++;
                //System.out.println(counter);
            }catch(Exception e) {
                //System.out.println("error: invalid formatting");
                setwarning("Invalid entry! Enter coordinate, space, direction. Example: J6 U");
                dir="up";
                col=row=-1;
            }
        }while(conflicts(col,row,dir,len,true)||dir.equals("error"));
        setwarning("");
        if(dir=="up") {
            while(len>0&&row>=0) {
                mygrid[col][row]=2;
                s.addcoord(new Coordinate(col,row));
                len--;
                row--;
            }
            s.coords.get(0).special='b';
            s.coords.get(s.holes-1).special='t';
        }
        if(dir=="down") {
            while(len>0&&row<=9) {
                mygrid[col][row]=2;
                s.addcoord(new Coordinate(col,row));
                len--;
                row++;
            }
            s.coords.get(0).special='t';
            s.coords.get(s.holes-1).special='b';
        }
        if(dir=="left") {
            while(len>0&&col>=0) {
                mygrid[col][row]=2;
                s.addcoord(new Coordinate(col,row));
                len--;
                col--;
            }
            s.coords.get(0).special='r';
            s.coords.get(s.holes-1).special='l';
        }
        if(dir=="right") {
            while (len > 0 && col <= 9) {
                mygrid[col][row] = 2;
                s.addcoord(new Coordinate(col, row));
                len--;
                col++;
            }
            s.coords.get(0).special = 'l';
            s.coords.get(s.holes - 1).special = 'r';
        }
        s.set=true;
    }

    public void setuserships() {
        boolean initialized=false;
        Initpanel frame;
        for(Ship s:myships) {
            frame = new Initpanel("Set your "+s.name+" ("+s.holes+" spaces long)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            int counter=0;
            int col;
            int row;
            String dir;
            int len;
            do {
                counter++;
                if(counter>1) {frame.setwarning("Invalid/overlapping location. Pick again!");}
                while(!initialized) {
                    timedelay(0.5);
                    //System.out.println("uninitialized");
                    if(frame.direction!=null&&frame.number!=-1&&frame.letter!='a') {
                        initialized=true;
                    }
                }
                //System.out.println("initialized now");
                initialized=false;
                col=frame.number;
                row=reverse(frame.letter);
                dir=frame.direction;
                len=s.getHoles();
            }while(conflicts(col,row,dir,len,true));
            initialized=false;
            if(dir=="up") {
                while(len>0&&row>=0) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row--;
                }
                s.coords.get(0).special='b';
                s.coords.get(s.holes-1).special='t';
            }
            if(dir=="down") {
                while(len>0&&row<=9) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row++;
                }
                s.coords.get(0).special='t';
                s.coords.get(s.holes-1).special='b';
            }
            if(dir=="left") {
                while(len>0&&col>=0) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col--;
                }
                s.coords.get(0).special='r';
                s.coords.get(s.holes-1).special='l';
            }
            if(dir=="right") {
                while(len>0&&col<=9) {
                    mygrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col++;
                }
                s.coords.get(0).special='l';
                s.coords.get(s.holes-1).special='r';
            }
            frame.dispose();
        }
    }

    public void setopponentships() {
        for(Ship s:opships) {
            int col=pickspot(10);
            int row=pickspot(10);
            String dir=directions[pickspot(4)];
            int len=s.getHoles();
            while(conflicts(col,row,dir,len,false)) {
                col=pickspot(10);
                row=pickspot(10);
                dir=directions[pickspot(4)];
                len=s.getHoles();
            }
            if(dir=="up") {
                while(len>0&&row>=0) {
                    opgrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row--;
                }
                s.coords.get(0).special='b';
                s.coords.get(s.holes-1).special='t';
            }
            if(dir=="down") {
                while(len>0&&row<=9) {
                    opgrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    row++;
                }
                s.coords.get(0).special='t';
                s.coords.get(s.holes-1).special='b';
            }
            if(dir=="left") {
                while(len>0&&col>=0) {
                    opgrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col--;
                }
                s.coords.get(0).special='r';
                s.coords.get(s.holes-1).special='l';
            }
            if(dir=="right") {
                while(len>0&&col<=9) {
                    opgrid[col][row]=2;
                    s.addcoord(new Coordinate(col,row));
                    len--;
                    col++;
                }
                s.coords.get(0).special='l';
                s.coords.get(s.holes-1).special='r';
            }
        }
//		for(Ship s:opships) {
//			s.printcoords();
//			System.out.println();
//		}
    }

    public Color gettilecolor(int code, boolean thisismygrid) {
        Color col=Color.BLUE;
        if(!thisismygrid&&gameinprogress) {return col;}
        if(code%2==0) {col=Color.GRAY;}
        return col;
    }

    public Color getholecolor(int code,boolean thisismygrid) {
        Color col=Color.blue;
        if(code==2) {
            if(thisismygrid||!gameinprogress) {col=Color.gray;}
        }
        if(code==3) {col=Color.white;}
        if(code==4) {col=Color.red;}
        return col;
    }

    public char convert(int row) {
        return (char)(65+row);
    }

    public int reverse(char row) {
        if(row>=97&&row<=106){row-=32;}
        return row-65;
    }

    public void timedelay(double time) {
        double start=System.currentTimeMillis();
        while(System.currentTimeMillis()<start+time*1000);
        //System.out.println("wait "+time+" second(s)");
    }

    public class textfieldlistener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            choice=enter.getText().trim();
            enter.setText("");
        }
    }

    public void checkifgameover() {
        if(myhitsleft>0&&ophitsleft>0) {
            //System.out.println("game continuing");
            return;
        }
        if(myhitsleft==0) {
            winner.append("Computer Wins!");
            computerWon = true;
        }else {
            winner.append(username+" Wins!");
            playerWon = true;
        }
        gameinprogress=false;
    }

    public char isthisacornerm(int x,int y) {
        Coordinate c=new Coordinate(x,y);
        for(Ship s:myships) {
            if(s.set){
                if(s.coords.get(0).equals(c)) {return s.coords.get(0).special;}
                if(s.coords.get(s.holes-1).equals(c)) {return s.coords.get(s.holes-1).special;}
            }
        }
        return 'n';
    }

    public char isthisacornero(int x,int y) {
        Coordinate c=new Coordinate(x,y);
        for(Ship s:opships) {
            //System.out.println(s.coords.get(0).special);
            if(s.coords.get(0).equals(c)) {return s.coords.get(0).special;}
            if(s.coords.get(s.holes-1).equals(c)) {return s.coords.get(s.holes-1).special;}
        }
        return 'n';
    }

    public void printGrid() {
        for(int x=0;x<10;x++) {
            for(int y=0;y<10;y++) {
                //System.out.print(opgrid[y][x] + "\t");
                if(y == 9) {
                    System.out.print("\n");
                }
            }
        }
        //System.out.println("\n");
    }

    public static void main(String[] args) {
        NoSaveState game=new NoSaveState("Player","12345");
        game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.setVisible(true);
        game.setResizable(true);
        //game.printGrid();
    }

}
