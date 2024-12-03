package example;

import example.domain.game.CaveGenerator;
import example.game.Game;
import example.server.Server;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        final var cave = CaveGenerator.generateUsingDrunkenWalk(60, 160);
        final var game = new Game(cave);
        final var server = new Server(game, Path.of("config/configuration.json"));
        server.start(8080);
    }
}
