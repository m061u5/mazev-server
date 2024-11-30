package example.game;

import example.domain.game.Action;
import example.domain.game.Cave;
import example.domain.game.Entity;
import example.domain.game.Location;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Game implements Function<Collection<Action>, Map<Entity, Location>> {
    private static final Random rg = new Random();
    private Map<Entity, Location> locations;
    private final Map<Entity.Player, Integer> playerHealth;
    private final Map<Entity.Player, Integer> playerGold;
    private final Cave cave;

    public Map<Entity, Location> locations() {
        return Collections.unmodifiableMap(locations);
    }

    public Cave cave() {
        return cave;
    }

    public Game(Cave cave) {
        this.cave = cave;
        this.locations = new HashMap<>();
        this.playerHealth = new HashMap<>();
        this.playerGold = new HashMap<>();
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

        for (final var entry : locations.entrySet()) {
            final var location = entry.getValue();
            tbl[location.row() * cave.columns() + location.column()] = switch (entry.getKey()) {
                case Entity.Gold ignored -> 'G';
                case Entity.Player ignored -> 'P';
                case Entity.Dragon ignored -> 'D';
                case Entity.Health ignored -> 'H';
            };
        }

        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                System.out.print(tbl[row * cave.columns() + column]);
            }
            System.out.println();
        }
    }

    public void addEntity(Entity entity, Supplier<Location> generateLocation) {
        for (; ; ) {
            final var location = generateLocation.get();
            if (locations.containsValue(location)) {
                continue;
            }

            locations.put(entity, location);
            if (entity instanceof Entity.Player player) {
                playerHealth.put(player, 100);
                playerGold.put(player, 0);
            }

            return;
        }
    }

    public Location randomLocation() {
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
        // make sure there is only one command per player
        final var firsts = commands.stream()
                .collect(Collectors.groupingBy(Action::player))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));

        // apply commands to player locations
        final var moved = locations.entrySet().stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey, entry -> {
                                    if (entry.getKey() instanceof Entity.Player player) {
                                        final var action = firsts.get(player);
                                        if (action == null) {
                                            return entry.getValue();
                                        }
                                        final var next = move(entry.getValue(), action);
                                        if (cave.rock(next.row(), next.column())) {
                                            return entry.getValue();
                                        }
                                        return next;
                                    }
                                    return entry.getValue();
                                }
                        )
                );

        // map location to list of entities at one
        final var locationEntities = moved.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // fight and collect gems
        final var updated = locationEntities.entrySet().stream()
                .flatMap(entry -> fight(entry.getValue()).map(entity -> Map.entry(entry.getKey(), entity)))
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        locations = updated;

        return updated;
    }

    private Stream<Entity> fight(List<Entity> entities) {
        final var players = entities.stream()
                .filter(entity -> entity instanceof Entity.Player)
                .map(entity -> (Entity.Player) entity)
                .toList();

        final var nonPlayers = entities.stream()
                .filter(entity -> !(entity instanceof Entity.Player)).toList();

        if (players.isEmpty()) {
            return nonPlayers.stream();
        }

        // "fight" - health of all players is reduced by half of health of the weakest one
        if (players.size() > 1) {
            final var optionalMinimum = players.stream().min((o1, o2) -> Integer.compare(playerHealth.get(o1), playerHealth.get(o2))).map(playerHealth::get);

            optionalMinimum.ifPresent(minimum -> players.forEach(player -> playerHealth.compute(player, (ignored, health) -> health - minimum / 2)));
        }

        final var optionalMaximum = players.stream().max((o1, o2) -> Integer.compare(playerHealth.get(o1), playerHealth.get(o2)));

        optionalMaximum.ifPresent(maximum -> {
            // do something with non player entities, i.e. gold?
            nonPlayers.forEach(entity -> {
                switch (entity) {
                    case Entity.Dragon dragon -> {
                    }

                    case Entity.Gold(int id, int value) -> {
                        playerGold.computeIfPresent(maximum, (ignored, current) -> {
                            return current + value;
                        });
                    }

                    case Entity.Health(int id, int value) -> {
                        playerHealth.computeIfPresent(maximum, (ignored, current) -> {
                            return current + value;
                        });
                    }

                    case Entity.Player ignored -> {
                    }
                }
            });
        });

        return players.stream().map(player -> (Entity) player);
    }

    private Location move(Location value, Action action) {
        return switch (action.direction()) {
            case Up -> new Location(value.row() - 1, value.column());
            case Down -> new Location(value.row() + 1, value.column());
            case Left -> new Location(value.row(), value.column() - 1);
            case Right -> new Location(value.row(), value.column() + 1);
        };
    }

    public int health(Entity.Player player) {
        return playerHealth.get(player);
    }

    public int gold(Entity.Player player) {
        return playerGold.get(player);
    }
}
