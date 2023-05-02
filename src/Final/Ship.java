package Final;

import java.util.*;

public class Ship{
    protected String name;
    protected int holes;
    protected int struck;
    protected boolean active;
    private ArrayList<Coordinate> coords;

    public Ship(String name,int holes) {
        this(name,holes,0,true);
    }

    public Ship(String name,int holes,int struck,boolean active){
        this.name=name;
        this.holes=holes;
        this.struck=struck;
        this.active=active;
        this.coords=new ArrayList<Coordinate>();
    }

    public void checkifstruck(Coordinate targ) {
        for(Coordinate c:coords) {
            if(targ.equals(c)) {
                struck++;
                break;
            }
        }
    }

    public void sunk() {
        active=false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHoles() {
        return holes;
    }

    public void setHoles(int holes) {
        this.holes = holes;
    }

    public int getStruck() {
        return struck;
    }

    public void setStruck(int struck) {
        this.struck = struck;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addcoord(Coordinate c) {
        this.coords.add(c);
    }

    public ArrayList<Coordinate> getCoords() {
        return coords;
    }

    public void setCoords(ArrayList<Coordinate> coords) {
        this.coords = coords;
    }

    public void printcoords() {
        for(Coordinate c:coords) {
            System.out.print(c.toString()+" ");
        }
    }
}
