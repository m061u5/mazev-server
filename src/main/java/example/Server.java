package example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final int PORT = 8080;
    private volatile Response.State currentState = new Response.State(0);
    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ReentrantLock and Condition for synchronization
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateUpdated = lock.newCondition();

    // SLF4J Logger
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        new Server().startServer();
    }

    public Server() {
    }

    public void startServer() {
        // Start the main processing thread
        Thread stateProcessor = Thread.startVirtualThread(this::processState);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port {}", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                // Handle each client in a separate virtual thread
                Thread.startVirtualThread(handler::run);
            }
        } catch (IOException e) {
            logger.error("Error in server socket operation", e);
        }
    }

    // Processes the state every second
    private void processState() {
        while (true) {
            try {
                // Wait for one second
                Thread.sleep(1000);

                // Process commands from the queue
                List<Command> commands = new ArrayList<>();
                commandQueue.drainTo(commands);

                // Update the state based on commands
                for (Command cmd : commands) {
                    logger.info("Processing command {}", cmd);
                    currentState = switch (cmd) {
                        case Command.Increment inc -> new Response.State(currentState.counter() + 1);
                        case Command.Decrement dec -> new Response.State(currentState.counter() - 1);
                    };
                }

                // Signal all client handlers that the state has been updated
                lock.lock();
                try {
                    stateUpdated.signalAll();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("State processing thread interrupted");
                break;
            }
        }
    }

    // Handles client connections
    private class ClientHandler {
        private final Socket socket;
        private final String clientId;
        private final BufferedReader in;
        private final PrintWriter out;
        private static final Logger clientLogger = LoggerFactory.getLogger(ClientHandler.class);

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.clientId = UUID.randomUUID().toString();
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            clientLogger.info("Client connected: {}", clientId);
        }

        public void run() {
            // Start a thread to read commands from the client
            Thread commandReader = Thread.startVirtualThread(this::readCommands);

            try {
                while (true) {
                    // Wait for the state to be updated
                    lock.lock();
                    try {
                        stateUpdated.await();
                    } finally {
                        lock.unlock();
                    }

                    // Send the new state to the client
                    sendState();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                clientLogger.warn("Client handler thread interrupted for client {}", clientId);
            } finally {
                // Clean up resources when the client disconnects
                clientLogger.info("Client disconnected: {}", clientId);
                try {
                    socket.close();
                } catch (IOException e) {
                    clientLogger.error("Error closing socket for client {}", clientId, e);
                }
            }
        }

        // Reads commands from the client
        private void readCommands() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    // Deserialize command
                    Command cmd = objectMapper.readValue(line, Command.class);
                    clientLogger.info("Received command from client {}: {}", clientId, cmd.getClass().getSimpleName());
                    commandQueue.offer(cmd);
                }
            } catch (IOException e) {
                clientLogger.error("Error reading commands from client {}", clientId, e);
            } finally {
                // Interrupt the main run loop to exit when the client disconnects
                Thread.currentThread().interrupt();
            }
        }

        public void sendState() {
            try {
                String stateJson = objectMapper.writeValueAsString(currentState);
                out.println(stateJson);
                clientLogger.info("Sent state to client {}: {}", clientId, currentState.counter());
            } catch (IOException e) {
                clientLogger.error("Error sending state to client {}", clientId, e);
            }
        }
    }
}
