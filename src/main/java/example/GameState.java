package example;

public class GameState {
    private int tickCount = 0;

    public String toJson() {
        return "{\"tickCount\": " + tickCount + "}";
    }

    public void processCommand(String command) {
        // Process a command received from a client
        System.out.println("Processing command: " + command);
    }

    public void update() {
        // Update game state at each cycle
        tickCount++;
        System.out.println("Game state updated: tick " + tickCount);
    }
}
