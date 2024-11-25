package example.game;

import example.domain.game.Entity;
import org.junit.jupiter.api.Test;

class GameTest {
    @Test
    void basic() {
        final var game = new Game(20, 20);
        game.addEntity(new Entity.Player("Player 0"));
        game.addEntity(new Entity.Player("Player 1"));
        game.addEntity(new Entity.Player("Player 2"));

        game.addEntity(new Entity.Gold(1, 10));
        game.addEntity(new Entity.Gold(2, 10));
        game.addEntity(new Entity.Gold(3, 10));
        game.addEntity(new Entity.Gold(4, 10));
        game.addEntity(new Entity.Gold(5, 10));
        game.render();
    }

}