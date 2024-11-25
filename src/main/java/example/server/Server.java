package example.server;

// Server.java

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.domain.Request;
import example.domain.Response;
import example.domain.game.Action;
import example.domain.game.Entity;
import example.game.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<Response.StateLocations> state = new AtomicReference<>(new Response.StateLocations(Set.of()));
    private final BlockingQueue<Action> actionsQueue = new LinkedBlockingQueue<>();
    private final Lock stateLock = new ReentrantLock();
    private final Condition stateUpdated = stateLock.newCondition();
    private final Game game;

    private final Map<Request.Authorize, Response> known = Map.of(
            new Request.Authorize("1234"),
            new Response.Authorized(new Entity.Player("Player 0"))
    );

    public Server(Game game) {
        this.game = game;
        game.addEntity(new Entity.Player("Player 0"));
    }

    public void start(int port) throws IOException {
        try (final var serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);

            // Start the commands processing thread
            Thread.startVirtualThread(this::processCommands);

            while (!Thread.currentThread().isInterrupted()) {
                final var clientSocket = serverSocket.accept();
                logger.info("Client connected: {}", clientSocket.getRemoteSocketAddress());

                // Start a client connection thread
                Thread.startVirtualThread(() -> handleClientConnection(clientSocket));
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (final var is = clientSocket.getInputStream();
             final var isr = new InputStreamReader(is);
             final var reader = new BufferedReader(isr);
             final var os = clientSocket.getOutputStream();
             final var osr = new OutputStreamWriter(os);
             final var writer = new BufferedWriter(osr)) {
            // handle authorization
            final var line = reader.readLine();
            if (line == null) {
                return;
            }

            final var request = objectMapper.readValue(line, Request.class);
            if (Objects.requireNonNull(request) instanceof Request.Authorize authorize) {
                final var response = known.getOrDefault(authorize, new Response.Unauthorized());
                final var json = objectMapper.writeValueAsString(response);
                writer.write(json);
                writer.newLine();
                writer.flush();

                if (response instanceof Response.Unauthorized) {
                    return;
                }
            } else {
                // ignore any other command
                return;
            }

            {
                final var json = objectMapper.writeValueAsString(new Response.StateCave(game.cave()));
                writer.write(json);
                writer.newLine();
                writer.flush();
            }

            Thread t1 = Thread.startVirtualThread(() -> handleClientCommands(reader));
            Thread t2 = Thread.startVirtualThread(() -> handleClientState(writer));
            t1.join();
            t2.join();
        } catch (IOException e) {
            logger.error("Commands processing thread interrupted", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Commands processing thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void processCommands() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Wait for one second
                Thread.sleep(1000);

                // Process all collected commands
                final var actions = new LinkedList<Action>();
                actionsQueue.drainTo(actions);

                final var locations = game.apply(actions);

                final var entityLocations = locations.entrySet().stream().map(entry -> new Response.StateLocations.EntityLocation(entry.getKey(), entry.getValue())).toList();
                // Update the state
                stateLock.lock();
                try {
                    state.set(new Response.StateLocations(entityLocations));
                    logger.info("State updated to {}", entityLocations);
                    // Notify client state threads
                    stateUpdated.signalAll();
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.error("Commands processing thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void handleClientCommands(BufferedReader reader) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) {
                    break;
                }

                final var request = objectMapper.readValue(line, Request.class);
                logger.info("Received command: {}", request);

                if (Objects.requireNonNull(request) instanceof Request.Command(Entity.Player.Direction direction)) {
                    actionsQueue.put(new Action(new Entity.Player("dummy"), direction));
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleClientState(BufferedWriter writer) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                stateLock.lock();
                try {
                    stateUpdated.await();
                    // Send the new state to the client
                    final var currentState = state.get();
                    final var stateJson = objectMapper.writeValueAsString(currentState);
                    writer.write(stateJson);
                    writer.newLine();
                    writer.flush();
                    logger.info("Sent state {}", stateJson);
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
