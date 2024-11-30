package example.game;

import example.domain.game.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameTest {
    private static class EmptyCave implements Cave {
        @Override
        public boolean rock(int row, int column) {
            return false;
        }

        @Override
        public int rows() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int columns() {
            return Integer.MAX_VALUE;
        }
    }

    @Test
    public void basic() {
        final var cave = new SimpleCave(20, 20);
        final var game = new Game(cave);

        for (final var s : Arrays.asList("Player 0", "Player 1", "Player 2")) {
            game.addEntity(new Entity.Player(s), game::randomLocation);
        }

        for (int i = 1; i < 6; i++) {
            game.addEntity(new Entity.Gold(i, 10), game::randomLocation);
        }
        game.render();
    }

    @Test
    public void twoPlayersFight() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Entity.Player("1");
        final var player2 = new Entity.Player("2");

        game.addEntity(player1, () -> new Location(1, 1));
        game.addEntity(player2, () -> new Location(3, 1));

        final var actions = List.of(new Action(player1, Entity.Player.Direction.Down), new Action(player2, Entity.Player.Direction.Up));
        final var expected = Map.of(player1, new Location(2, 1), player2, new Location(2, 1));
        final var actual = game.apply(actions);

        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(50, game.health(player1));
        Assertions.assertEquals(50, game.health(player2));
    }

    @Test
    public void onlyOnePlayerMoves() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Entity.Player("1");
        final var player2 = new Entity.Player("2");
        final var health = new Entity.Health(0, 10);

        game.addEntity(player1, () -> new Location(1, 1));
        game.addEntity(player2, () -> new Location(3, 1));
        game.addEntity(health, () -> new Location(2, 1));

        final var actions = List.of(new Action(player1, Entity.Player.Direction.Down));

        final var expected = Map.of(player1, new Location(3, 1), player2, new Location(3, 1));

        final var actual0 = game.apply(actions);
        final var actual = game.apply(actions);


        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(60, game.health(player1));
        Assertions.assertEquals(50, game.health(player2));
    }

    @Test
    public void playerMoves() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Entity.Player("1");

        game.addEntity(player1, () -> new Location(1, 1));

        final var actions = List.of(new Action(player1, Entity.Player.Direction.Down));
        final var expected = Map.of(player1, new Location(2, 1));
        final var actual = game.apply(actions);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void playerGetsGold() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Entity.Player("1");
        final var gold1 = new Entity.Gold(0, 10);

        game.addEntity(player1, () -> new Location(1, 1));
        game.addEntity(gold1, () -> new Location(2, 1));

        final var actions = List.of(new Action(player1, Entity.Player.Direction.Down));
        final var expected = Map.of(player1, new Location(2, 1));
        final var actual = game.apply(actions);

        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(gold1.value(), game.gold(player1));
    }

}