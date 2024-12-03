package example.domain.game;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class CaveGenerator {
    public static Cave generateUsingCellularAutomata(int rows, int columns) {
        final var rocks = new boolean[rows * columns];

        initializeRandomly(rocks, rows, columns);
        for (int i = 0; i < 5; i++) {
            iterate(rocks, rows, columns);
        }
        border(rocks, rows, columns);

        return new SimpleCave(rocks, rows, columns);
    }

    private static void border(boolean[] rocks, int rows, int columns) {
        for (int row = 0; row < rows; row++) {
            rocks[row*columns] =  true;
            rocks[row*columns + columns - 1] = true;
        }
        Arrays.fill(rocks, 0, columns, true);
        Arrays.fill(rocks, (rows - 1) * columns, rows * columns, true);
    }

    private static void initializeRandomly(boolean[] rocks, int rows, int columns) {
        final var rg = ThreadLocalRandom.current();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (0 < column && column < columns - 1 && 0 < row && row < rows - 1) {
                    rocks[row * columns + column] = rg.nextFloat() > 0.65;
                } else {
                    rocks[row * columns + column] = true;
                }
            }
        }
    }

    private static int neighbours(boolean[] rocks, int rows, int columns, int row, int column) {
        return rock1(rocks, rows, columns, row - 1, column - 1) + rock1(rocks, rows, columns, row, column - 1) + rock1(rocks, rows, columns, row + 1, column - 1) +
                rock1(rocks, rows, columns, row - 1, column) + rock1(rocks, rows, columns, row + 1, column) +
                rock1(rocks, rows, columns, row - 1, column + 1) + rock1(rocks, rows, columns, row, column + 1) + rock1(rocks, rows, columns, row + 1, column + 1);
    }

    private static void iterate(boolean[] rocks, int rows, int columns) {
        final var next = new boolean[rocks.length];

        for (int j = 1; j < rows - 1; j++) {
            for (int i = 1; i < columns - 1; i++) {
                final var nrocks = neighbours(rocks, rows, columns, j, i);
                if (!rocks[j * columns + i] && nrocks <= 4) { // passage
                    next[j * columns + i] = false;
                } else if (rocks[j * columns + i] && nrocks <= 2) {
                    next[j * columns + i] = false;
                } else {
                    next[j * columns + i] = true;
                }
            }
        }

        System.arraycopy(next, 0, rocks, 0, rocks.length);
    }

    private static int rock1(boolean[] rocks, int rows, int columns, int j, int i) {
        return rocks[j*columns + i] ? 1 : 0;
    }

    public static Cave generateUsingDrunkenWalk(int rows, int columns) {
        final var rocks = new boolean[columns * rows];

        Arrays.fill(rocks, true);
        initialize(rocks, rows, columns);
        border(rocks, rows, columns);

        return new SimpleCave(rocks, rows, columns);
    }

    private static void initialize(boolean[] rocks, int rows, int columns) {
        final var rg = ThreadLocalRandom.current();
        var row = rows / 2;
        var column = columns / 2;
        var n = (rows * columns * 8) / 10;
        while (n > 0) {
            if (rocks[row*columns + column]) {
                rocks[row*columns + column] = false;
                n--;
            }

            switch (rg.nextInt(4)) {
                case 0:
                    row--;
                    break;
                case 1:
                    row++;
                    break;
                case 2:
                    column--;
                    break;
                case 3:
                    column++;
                    break;
            }

            row = Math.max(Math.min(row, rows - 1), 0);
            column = Math.max(Math.min(column, columns - 1), 0);
        }
    }

}

