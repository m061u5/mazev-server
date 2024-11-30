package example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends AbstractVerticle {
    private ConcurrentHashMap<ServerWebSocket, String> clients = new ConcurrentHashMap<>();
    private GameState gameState = new GameState();

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new GameServer());
    }

    @Override
    public void start() {
        vertx.createHttpServer().webSocketHandler(ws -> {
            clients.put(ws, "");
            ws.textMessageHandler(message -> {
                // Handle command from the client
                handleClientCommand(ws, message);
            }).closeHandler(v -> {
                clients.remove(ws);
            });
        }).listen(8080, res -> {
            if (res.succeeded()) {
                System.out.println("Server listening on port 8080");
            } else {
                System.out.println("Failed to start server: " + res.cause());
            }
        });

        // Schedule periodic state updates
        vertx.setPeriodic(1000, id -> {
            // Send the game state to all clients
            broadcastGameState();
            processClientCommands();
            updateGameState();
        });
    }

    private void broadcastGameState() {
        String state = gameState.toJson();
        clients.keySet().forEach(ws -> ws.writeTextMessage(state));
    }

    private void handleClientCommand(ServerWebSocket ws, String command) {
        // Store the client's command
        clients.put(ws, command);
    }

    private void processClientCommands() {
        // Process all commands received from clients
        for (String command : clients.values()) {
            if (!command.isEmpty()) {
                gameState.processCommand(command);
            }
        }
        clients.replaceAll((ws, v) -> ""); // Clear commands after processing
    }

    private void updateGameState() {
        gameState.update();
    }
}
