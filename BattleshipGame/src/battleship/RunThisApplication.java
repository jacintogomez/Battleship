package battleship;

import javax.swing.JFrame;

public class RunThisApplication {

	public static void main(String[] args) {
		Server sv = new Server();
		sv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    sv.setVisible(true);
		Player pl = new Player();
	}

}
