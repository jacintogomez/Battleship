package Final;

import java.util.*;

public class Player{
    String username;
    String password;
    int id;
    int numberofwins;

    public Player(String username,String password,int id) {
        this(username,password,id,0);
    }

    public Player(String username,String password,int id,int numberofwins) {
        this.username=username;
        this.password=password;
        this.id=id;
        this.numberofwins=numberofwins;
    }

}