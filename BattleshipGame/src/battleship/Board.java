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

public class Board extends JFrame implements Runnable, Serializable, ActionListener {
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

	private JButton btnOpenConnection, btnCloseConnection, btnSaveGame, btnDeleteGame, btnContinueGame, btnNewGame, btnLoadGame, 
		btnWinLossRecord;
	private JLabel deleteGameLabel,warning,enterlabel, newLoadGameLabel, noLoadedGameLabel;
	private JFrame frameDeleteGame, frameEndGame, frameWinsLosses, frameNewLoad;
	private JTextField enter;
	private String choice,lastchoice;
	private JTextArea messages, winner;
	private JScrollPane scroll;

	private ArrayList<Ship> myships=new ArrayList<Ship>();
	private ArrayList<Ship> opships=new ArrayList<Ship>();
	private ImagePanel leftboard;
	private ImagePanel rightboard;
	private String username="jacinto";
	private String password="1234";
	private int ophitsleft;
	private int myhitsleft;

	private Socket socket;
	ObjectOutputStream toServer = null;
	ObjectInputStream fromServer = null;

	public Board() {
		this.username = "Jacinto";
		this.password = "12345";
		for(int x=0;x<10;x++) {
			for(int y=0;y<10;y++) {
				mygrid[x][y]=1;
				opgrid[x][y]=1;
			}
		}
		ophitsleft=myhitsleft=17;
		createships();
		setopponentships();
		setuserships();
		//randomizeuserships();
		launchgame();
	}

	public Board(String username,String password) {
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
		try {
			socket = new Socket("localhost", 8000);
			try {
		    	  toServer = new ObjectOutputStream(socket.getOutputStream());
			      fromServer = new ObjectInputStream(socket.getInputStream());

			    }
			    catch (IOException ex) {
			      messages.append(ex.toString() + '\n');
			    }
			messages.append("connected\n");
		} catch (IOException e1) {
			e1.printStackTrace();
			messages.append("connection Failure\n");
		}
	}

	public Board(String username,String password,int mygrid[][],int opgrid[][],ArrayList<Ship> myships,
			ArrayList<Ship> opships,int myhitsleft,int ophitsleft,boolean launchGame, Queue<Coordinate> attack,
			boolean shipsset) {
		this.shipsset= shipsset;
		this.loadedGame = true;
		this.username=username;
		this.password=password;
		this.mygrid=mygrid;
		this.opgrid=opgrid;
		this.myships=myships;
		this.opships=opships;
		this.myhitsleft=myhitsleft;
		this.ophitsleft=ophitsleft;
		this.attack=(LinkedList)attack;
//		for(Coordinate s : this.attack) {
//			  System.out.println(s.toString());
//		}
//		System.out.println("done with first");
//		for(Coordinate s : attack) {
//			  System.out.println(s.toString());
//		}
//		System.out.println("done with second");
		if(launchGame == true) {
			launchgame();
			try {
				socket = new Socket("localhost", 8000);
				try {
			    	  toServer = new ObjectOutputStream(socket.getOutputStream());
				      fromServer = new ObjectInputStream(socket.getInputStream());

				    }
				    catch (IOException ex) {
				      messages.append(ex.toString() + '\n');
				    }
				messages.append("connected\n");
			} catch (IOException e1) {
				e1.printStackTrace();
				messages.append("connection Failure\n");
			}
		}

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
		JPanel savePanel = new JPanel(new GridLayout(1,2));
		btnSaveGame = new JButton("Save Game");
		btnSaveGame.addActionListener(this);
		JButton btnExitGame = new JButton("Exit game");
		btnExitGame.addActionListener(this);
		savePanel.add(btnExitGame);
		savePanel.add(btnSaveGame);
		JPanel viewRecordPanel = new JPanel();
		JButton btnViewRecords = new JButton("View Win/Loss Record");
		btnViewRecords.addActionListener(this);
		viewRecordPanel.add(btnViewRecords);


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
		topPanel.add(viewRecordPanel, BorderLayout.CENTER);
		topPanel.add(savePanel, BorderLayout.WEST);
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

	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();


		if(cmd.equals("Open Connection")) {
			try {
				socket = new Socket("localhost", 8000);
				try {
			    	  toServer = new ObjectOutputStream(socket.getOutputStream());
				      fromServer = new ObjectInputStream(socket.getInputStream());

				    }
				    catch (IOException ex) {
				      messages.append(ex.toString() + '\n');
				    }
				messages.append("connected\n");
			} catch (IOException e1) {
				e1.printStackTrace();
				messages.append("connection Failure\n");
			}
		}
		else if(cmd.equals("Close Connection")) {
			try {
				if(socket != null) {
					socket.close();
				}
				if(toServer != null) {
					toServer.close();
				}
				if(fromServer != null) {
					fromServer.close();
				}

				messages.append("connection closed\n");
			} catch (Exception e1) {
				System.err.println("error");
			}
		}


		else if(cmd.equals("Save Game")) {
			saveGame();
		}
		else if(cmd.equals("Yes, delete previous saved game")) {
			frameDeleteGame.dispose();
			deleteGame();

		}
		else if(cmd.equals("No, continue current game")) {
			frameDeleteGame.dispose();
		}
		else if(cmd.equals("Exit game")) {
			/*
			try {
				if(socket != null) {
					socket.close();
				}
				if(toServer != null) {
					toServer.close();
				}
				if(fromServer != null) {
					fromServer.close();
				}

			} catch (Exception e1) {
				System.err.println("error");
			}
			*/
			if(playerWon == true || computerWon == true) {
				frameEndGame.dispose();
			}
			this.dispose();
			newOrLoadGameUI();
			//System.exit(0);
		}
		else if(cmd.equals("View Win/Loss Record")) {
			getNumWinsLosses();
		}
		else if(cmd.equals("Close")) {
			frameWinsLosses.dispose();
		}
		else if(cmd.equals("Start New Game")) {
			//frameEndGame.dispose();
			//this.dispose();
			try { 
				if(socket != null) {
					socket.close();
				}
				if(toServer != null) {
					toServer.close();
				}
				if(fromServer != null) {
					fromServer.close();
				}

				//ta.append("connection closed\n");
			} catch (Exception e1) {
				System.err.println("error"); 
			}
			frameNewLoad.dispose();
			Board newGame = new Board(this.username, this.password);
			newGame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			newGame.setLocationRelativeTo(null);
		    newGame.setVisible(true);
		    newGame.setResizable(true);
		}
		else if(cmd.equals("Load Existing Game")) {
			Board loadedGame = loadGame();
			if(loadedGame != null) {
				try { 
					if(socket != null) {
						socket.close();
					}
					if(toServer != null) {
						toServer.close();
					}
					if(fromServer != null) {
						fromServer.close();
					}

					//ta.append("connection closed\n");
				} catch (Exception e1) {
					System.err.println("error"); 
				}
				frameNewLoad.dispose();
				String tempUsername = loadedGame.getUsername();
				String tempPassword = loadedGame.getPassword();
				int[][] tempMygrid = loadedGame.getMyGrid();
				int[][] tempOpgrid = loadedGame.getOpGrid();
				ArrayList<Ship> tempMyships = loadedGame.getMyships();
				ArrayList<Ship> tempOpships = loadedGame.getOpships();
				int tempMyhitsleft = loadedGame.getMyhitsleft();
				int tempOphitsleft = loadedGame.getOphitsleft();
				Queue<Coordinate> tempAttack = loadedGame.getAttack();
				boolean tempShipsset = loadedGame.getShipsset();
				Board newGame = new Board(tempUsername,tempPassword,tempMygrid,tempOpgrid,tempMyships,
						tempOpships,tempMyhitsleft,tempOphitsleft, true, tempAttack, tempShipsset);
				newGame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				newGame.setLocationRelativeTo(null);
			    newGame.setVisible(true);    
			    newGame.setResizable(true);
				//saveGameUI();
			}
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
			updateWinLoss();
			deleteGame();
			endGameUI();
		}else {
			winner.append(username+" Wins!");
			playerWon = true;
			updateWinLoss();
			deleteGame();
			endGameUI();
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

	public void saveGame() {
		if(shipsset == false) {
			messages.insert("You cannot save this game until you have added all 5 ships to your board!\n",0);
		}
		else {
			String messageType = "save";
			//this.setGridCell(5, 5, setNum);
			//setNum++;
			//this.printGrid();
			//System.out.println("save");
			ArrayList<Object> messageArray = new ArrayList<>();
			messageArray.add(messageType);
			messageArray.add(this.loadedGame);
			messageArray.add(this.username);
			messageArray.add(this.password);
			messageArray.add(this.mygrid);
			messageArray.add(this.opgrid);
			messageArray.add(this.myships);
			messageArray.add(this.opships);
			messageArray.add(this.myhitsleft);
			messageArray.add(this.ophitsleft);
			messageArray.add(this.attack);
			messageArray.add(this.shipsset);
	    	try {
				toServer.writeObject(messageArray);
				toServer.flush();
	
		        Object object = null;
				object = fromServer.readObject();
				ArrayList<Object> retArr = (ArrayList<Object>)object;
	
				String gameString = (String)retArr.get(0);
				if(gameString.equals("You have a game saved.\nYou can only have 1 game saved at a time.\nPlease either delete this game in order to save the current game, or continue playing the current game.\n")) {
					deleteGameUI();
				}
				else if(gameString.equals("An earlier version of this game is saved. Updating game to current state.")) {
					messages.insert(gameString.toString() + "\n",0);
					updateGame();
				}
				else {
					messages.insert(gameString.toString() + "\n",0);
				}
	
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateGame() {
		String messageType = "update";
		ArrayList<Object> messageArray = new ArrayList<>();
		messageArray.add(messageType);
		messageArray.add(this.username);
		messageArray.add(this.password);
		messageArray.add(this.mygrid);
		messageArray.add(this.opgrid);
		messageArray.add(this.myships);
		messageArray.add(this.opships);
		messageArray.add(this.myhitsleft);
		messageArray.add(this.ophitsleft);
		messageArray.add(this.attack);
		messageArray.add(this.shipsset);

		try {
			toServer.reset();
			toServer.writeObject(messageArray);
			toServer.flush();

	        Object object = null;
			object = fromServer.readObject();
			ArrayList<Object> retArr = (ArrayList<Object>)object;

			String gameString = (String)retArr.get(0);
			messages.insert(gameString.toString() + "\n",0);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Board loadGame() {
		String messageType = "load";
		ArrayList<Object> messageArray = new ArrayList<>();
		messageArray.add(messageType);
		String[] unamePword = new String[2];
		unamePword[0] = this.username;
		unamePword[1] = this.password;
		messageArray.add(unamePword);
		Board loadedGame = null;
	
    	try {
			toServer.writeObject(messageArray);
			toServer.flush();
			
			String tempStr = "You do not have a game saved. Please start a new game!";
	        Object object = null;
			
			object = fromServer.readObject();
			ArrayList<Object> retArr = (ArrayList<Object>)object;
			String gameString = (String)retArr.get(0);
			if(gameString.equals("You do not have a game saved.\nPlease start a new game!")) {
				noLoadedGameLabel.setText(tempStr);
			}
			else {
				loadedGame = (Board)retArr.get(1);
				//ta.append(gameString.toString());	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    	
    	return loadedGame;
	}
	
	public void deleteGame() {
		String messageType = null;
		if(computerWon == true || playerWon == true) {
			messageType = "delete";
		}
		else {
			messageType = "deleteandsave";
		}
		ArrayList<Object> messageArray = new ArrayList<>();
		messageArray.add(messageType);
		messageArray.add(this.username);
		messageArray.add(this.password);
		messageArray.add(this.mygrid);
		messageArray.add(this.opgrid);
		messageArray.add(this.myships);
		messageArray.add(this.opships);
		messageArray.add(this.myhitsleft);
		messageArray.add(this.ophitsleft);
		messageArray.add(this.attack);
		messageArray.add(this.shipsset);
    	try {
    		if(messageType.equals("delete")) {
    			toServer.reset();
    		}
			toServer.writeObject(messageArray);
			toServer.flush();

	        Object object = null;
			object = fromServer.readObject();
			ArrayList<Object> retArr = (ArrayList<Object>)object;

			String gameString = (String)retArr.get(0);
			messages.insert(gameString.toString() + "\n",0);


		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void updateWinLoss() {
		String messageType = "updateWinLoss";
		ArrayList<Object> messageArray = new ArrayList<>();
		messageArray.add(messageType);
		messageArray.add(this.username);
		messageArray.add(this.password);
		messageArray.add(this.playerWon);
		try {
			toServer.writeObject(messageArray);
			toServer.flush();

	        Object object = null;
			object = fromServer.readObject();
			ArrayList<Object> retArr = (ArrayList<Object>)object;

			String gameString = (String)retArr.get(0);
			messages.append(gameString + "\n");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void endGameUI() {
		frameEndGame = new JFrame();
		frameEndGame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameEndGame.setSize(400,100);
		frameEndGame.setLayout(new BorderLayout());
		JLabel endGameLabel = null;
		if(playerWon == true) {
			endGameLabel = new JLabel("Congratulations! You won!", SwingConstants.CENTER);
		}
		else {
			endGameLabel = new JLabel("Sorry! Better luck next time!", SwingConstants.CENTER);
		}
		JPanel topPanel = new JPanel();
		topPanel.add(endGameLabel);
		JButton btnExitGame = new JButton("Exit game");
		JButton btnStartGame = new JButton("Start new game");
		btnStartGame.addActionListener(this);
		btnExitGame.addActionListener(this);
		JPanel bottomPanel = new JPanel(new GridLayout(1,1));
		//bottomPanel.add(btnStartGame);
		bottomPanel.add(btnExitGame);

		frameEndGame.add(topPanel, BorderLayout.NORTH);
		frameEndGame.add(bottomPanel,BorderLayout.CENTER);
		frameEndGame.setLocationRelativeTo(null);
		frameEndGame.setVisible(true);
	}

	private void deleteGameUI()
	{
		frameDeleteGame = new JFrame();
		frameDeleteGame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameDeleteGame.setSize(600,100);
		//Layout of Main Window
		frameDeleteGame.setLayout(new BorderLayout());

		JLabel deleteGameLabelOne = new JLabel("<html>You already have a different game saved.</html>",SwingConstants.CENTER);
		JLabel deleteGameLabelTwo = new JLabel("<html>Do you want to delete this existing game and save your current game in its place?</html>",SwingConstants.CENTER);
		JPanel pnlLabel = new JPanel(new GridLayout(2,1));
		pnlLabel.add(deleteGameLabelOne);
		pnlLabel.add(deleteGameLabelTwo);

		btnDeleteGame = new JButton("Yes, delete previous saved game");
		btnDeleteGame.addActionListener(this);

		btnContinueGame = new JButton("No, continue current game");
		btnContinueGame.addActionListener(this);


		JPanel pnlButton = new JPanel(new GridLayout(1,2));

		pnlButton.add(btnDeleteGame);
		pnlButton.add(btnContinueGame);



		frameDeleteGame.add(pnlLabel, BorderLayout.NORTH);
		frameDeleteGame.add(pnlButton, BorderLayout.CENTER);
		frameDeleteGame.setLocationRelativeTo(null);

		//frame.pack();
		frameDeleteGame.setVisible(true);
	}

	

	public void getNumWinsLosses() {
		String messageType = "WinsandLosses";
		ArrayList<Object> messageArray = new ArrayList<>();
		messageArray.add(messageType);
		messageArray.add(this.username);

		try {
			toServer.writeObject(messageArray);
			toServer.flush();

	        Object object = null;

			object = fromServer.readObject();

			ArrayList<Object> retArr = (ArrayList<Object>)object;
			int numWins = (int)retArr.get(0);
			int numLosses = (int)retArr.get(1);

			createWinsLossesUI(numWins, numLosses);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public void createWinsLossesUI(int numWins, int numLosses) {
		frameWinsLosses = new JFrame();
		frameWinsLosses.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameWinsLosses.setSize(400,200);
		frameWinsLosses.setLayout(new BorderLayout());
		String forTitleLabel = username + "'s Win/Loss Record";
		JLabel titleLabel = new JLabel(forTitleLabel, SwingConstants.CENTER);
		JPanel topPanel = new JPanel();
		topPanel.add(titleLabel);

		JPanel midPanel = new JPanel(new GridLayout(3,2));
		JLabel numWinsLabel = new JLabel("Number of wins: ");
		JLabel numLossesLabel = new JLabel("Number of losses: ");
		JLabel numgamesLabel = new JLabel("Total games completed: ");
		JLabel numWinsLabelVal = new JLabel(String.valueOf(numWins), SwingConstants.CENTER);
		JLabel numLossesLabelVal = new JLabel(String.valueOf(numLosses), SwingConstants.CENTER);
		JLabel numgamesLabelVal = new JLabel(String.valueOf(numWins + numLosses), SwingConstants.CENTER);

		midPanel.add(numWinsLabel);
		midPanel.add(numWinsLabelVal);
		midPanel.add(numLossesLabel);
		midPanel.add(numLossesLabelVal);
		midPanel.add(numgamesLabel);
		midPanel.add(numgamesLabelVal);

		JPanel bottomPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		bottomPanel.add(closeButton);

		frameWinsLosses.add(topPanel, BorderLayout.NORTH);
		frameWinsLosses.add(midPanel, BorderLayout.CENTER);
		frameWinsLosses.add(bottomPanel, BorderLayout.SOUTH);
		frameWinsLosses.setLocationRelativeTo(null);
		frameWinsLosses.setVisible(true);
	}
	
	private void newOrLoadGameUI()
	{
		frameNewLoad = new JFrame();
		frameNewLoad.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameNewLoad.setSize(600,125);
		//Layout of Main Window
		frameNewLoad.setLayout(new BorderLayout());
		
		newLoadGameLabel = new JLabel("Start a New Game, Load an Existing Game, or View Your Win/Loss Record");
		JPanel pnlLabel = new JPanel();
		pnlLabel.add(newLoadGameLabel);
		btnNewGame = new JButton("Start New Game");
		btnNewGame.addActionListener(this);
		
		btnLoadGame = new JButton("Load Existing Game");
		btnLoadGame.addActionListener(this);
		
		btnWinLossRecord = new JButton("View Win/Loss Record");
		btnWinLossRecord.addActionListener(this);
		JPanel pnlButton = new JPanel(new GridLayout(1,3));
		
		pnlButton.add(btnNewGame);
		pnlButton.add(btnLoadGame);
		pnlButton.add(btnWinLossRecord);
		
		JPanel pnlBottom = new JPanel();
		noLoadedGameLabel = new JLabel(" ");
		noLoadedGameLabel.setForeground(Color.RED);
		pnlBottom.add(noLoadedGameLabel);
		
		frameNewLoad.add(pnlLabel, BorderLayout.NORTH);
		frameNewLoad.add(pnlButton, BorderLayout.CENTER);
		frameNewLoad.add(pnlBottom, BorderLayout.SOUTH);
		
		//frame.pack();
		frameNewLoad.setLocationRelativeTo(null);
		frameNewLoad.setVisible(true);
	}


	public static void main(String[] args) {
		Board game=new Board("Player","12345");
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    game.setVisible(true);
	    game.setResizable(true);
	    //game.printGrid();
	}

}
