package example.domain.game;

public class Cave {
    public final int columns;
    public final int rows;
    public final boolean[] rocks;

    private Cave() {
        this.columns = 0;
        this.rows = 0;
        this.rocks = new boolean[0];
    }

    public Cave(int rows, int columns) {
        this.columns = columns;
        this.rows = rows;
        this.rocks = new boolean[columns * rows];
    }
    
    public boolean rock(int row, int column) {
        return rocks[row * columns + column];
    }
    
    public void set(int row, int column, boolean value) {
        rocks[row * columns + column] = value;
    }
    
    public int rows() {
        return this.rows;
    }
    
    public int columns() {
        return this.columns;
    }
}

