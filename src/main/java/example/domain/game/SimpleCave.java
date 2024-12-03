package example.domain.game;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public final class SimpleCave implements Cave {
    @JsonProperty
    private final int columns;

    @JsonProperty
    private final int rows;

    @JsonProperty
    private final boolean[] rocks;

    private SimpleCave() {
        this.columns = 0;
        this.rows = 0;
        this.rocks = new boolean[0];
    }

    public SimpleCave(boolean[] rocks, int rows, int columns) {
        this.columns = columns;
        this.rows = rows;
        this.rocks = Arrays.copyOf(rocks, rocks.length);
    }

    @Override
    public boolean rock(int row, int column) {
        return this.rocks[row*this.columns + column];
    }

    @Override
    public int rows() {
        return this.rows;
    }

    @Override
    public int columns() {
        return this.columns;
    }
}
