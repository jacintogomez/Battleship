package battleship;

import java.io.Serializable;

public class Coordinate implements Serializable {
	protected char row;
	protected int column;
	protected int rawcolumn;
	protected int rawrow;
	protected char special;
	private int nothing;
	
	public Coordinate(int column,int row) {
		this.rawrow=row;
		this.rawcolumn=column;
		this.row=convert(row);
		this.column=column+1;
		this.special='n';
		//n=normal,l=left,r=right,t=top,b=bottom
	}
	
	public char convert(int row) {
		return (char)(65+row);
	}
	
	public int reverse(char row) {
		return row-65;
	}
	
	public char getRow() {
		return row;
	}

	public void setRow(char row) {
		this.row = row;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public String toString() {
		return "["+row+","+column+"]";
	}
	
	public boolean equals(Coordinate c) {
		return this.row==c.row&&this.column==c.column;
	}
}
