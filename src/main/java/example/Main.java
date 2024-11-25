package example;

import example.domain.game.Cave;
import example.game.Game;
import example.server.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        final var cave = new Cave(20, 20);
        final var game = new Game(cave);
        final var server = new Server(game);
        server.start(8080);
    }
}
