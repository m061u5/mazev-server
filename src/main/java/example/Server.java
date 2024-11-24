package example;

// Server.java

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<State> state = new AtomicReference<>(new State(0));
    private static final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private static final Lock stateLock = new ReentrantLock();
    private static final Condition stateUpdated = stateLock.newCondition();

    public static void main(String[] args) throws IOException {
        final var server = new Server();
        server.start(8080);
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
                final var commands = new LinkedList<Command>();
                commandQueue.drainTo(commands);

                // Update the state
                var newValue = state.get().value();
                for (final var command : commands) {
                    newValue = switch (command) {
                        case Increment ic -> newValue + 1;
                        case Decrement dc -> newValue - 1;
                    };
                }

                // Update the state
                stateLock.lock();
                try {
                    state.set(new State(newValue));
                    logger.info("State updated to {}", newValue);
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
        try  {
            while (!Thread.currentThread().isInterrupted()) {
                 final var line = reader.readLine();
                 if (line == null) {
                     break;
                 }

                final var command = objectMapper.readValue(line, Command.class);
                logger.info("Received command: {}", command);

                commandQueue.put(command);
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
