package example.game;

import example.domain.Response;
import example.domain.game.Action;
import example.domain.game.Cave;
import example.domain.game.Entity;
import example.domain.game.Location;

import java.util.*;
import java.util.function.Function;

public class Game implements Function<Collection<Action>, Map<Entity, Location>> {
    private static final Random rg = new Random();
    private final Map<Entity, Location> entities;
    private final Cave cave;

    public Map<Entity, Location> entities() {
        return Collections.unmodifiableMap(entities);
    }

    public Cave cave() {
        return cave;
    }

    public Game(Cave cave) {
        this.cave = cave;
        initialize();
        this.entities = new HashMap<>();
    }

    public void render() {
        final var tbl = new char[cave.columns() * cave.rows()];
        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                if (cave.rock(row, column)) {
                    tbl[row * cave.columns() + column] = 'X';
                } else {
                    tbl[row * cave.columns() + column] = ' ';
                }
            }
        }

        for (final var entry : entities.entrySet()) {
            final var location = entry.getValue();
            tbl[location.row() * cave.columns() + location.column()] = switch (entry.getKey()) {
                case Entity.Gold ignored -> 'G';
                case Entity.Player ignored -> 'P';
                case Entity.Dragon dragon -> 'D';
            };
        }

        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                System.out.print(tbl[row * cave.columns() + column]);
            }
            System.out.println();
        }
    }

    private void initialize() {
        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                cave.set(row, column, !(0 < column && column < cave.columns() - 1 && 0 < row && row < cave.rows() - 1 && rg.nextDouble() < 0.8));
            }
        }
    }

    public void addEntity(Entity entity) {
        for (; ; ) {
            final var location = generateLocation();
            if (entities.containsValue(location)) {
                continue;
            }

            entities.put(entity, location);
            return;
        }
    }

    private Location generateLocation() {
        for (; ; ) {
            final var row = rg.nextInt(cave.rows());
            final var column = rg.nextInt(cave.columns());
            if (cave.rock(row, column)) {
                continue;
            }

            return new Location(row, column);
        }
    }

    @Override
    public Map<Entity, Location> apply(Collection<Action> commands) {
        return Collections.unmodifiableMap(entities);
    }
}
