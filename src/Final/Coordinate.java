package Final;

public class Coordinate{
    char row;
    int column;

    public Coordinate(int column,int row) {
        this.row=convert(row);
        this.column=column+1;
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