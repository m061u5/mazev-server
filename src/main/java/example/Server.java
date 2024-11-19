package example;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final Logger logger = LoggerFactory. getLogger(Server.class);
    private static final String brokerURL = "tcp://0.0.0.0:8080";

    private double state = 100.0;

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startBroker();
        server.run();
    }

    public void startBroker() throws Exception {
        // Start the embedded broker
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector(brokerURL);
        broker.start();
        logger.info("Embedded broker started at {}", brokerURL);
    }

    public void run() throws Exception {
        // Create a connection factory
        final var connectionFactory = new ActiveMQConnectionFactory(brokerURL);

        // Create a connection
        final var connection = connectionFactory.createConnection();
        connection.start();

        // Create a session (non-transacted, auto acknowledge)
        final var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create the destination (Topic)
        final var stateTopic = session.createTopic("stateTopic");

        // Create a producer for the stateTopic
        final var stateProducer = session.createProducer(stateTopic);
        stateProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        // Create the commandQueue for receiving commands from clients
        final var commandQueue = session.createQueue("commandQueue");

        // Create a consumer for the commandQueue
        final var commandConsumer = session.createConsumer(commandQueue);

        // Set a listener to process incoming commands
        commandConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage commandText) {
                    processCommand(commandText.getText());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Schedule the state updates every one second
        try (final var scheduler = Executors.newScheduledThreadPool(1)) {

            Runnable sendStateTask = () -> {
                try {
                    sendState(session, stateProducer);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            };

            scheduler.scheduleAtFixedRate(sendStateTask, 0, 1, TimeUnit.SECONDS);

            // Keep the main thread alive
            Thread.sleep(Long.MAX_VALUE);
        }
    }

    private void sendState(Session session, MessageProducer producer) throws JMSException {
        // Create a state message
        final var message = session.createTextMessage(Double.toString(state));
        // Send the state message
        producer.send(message);
        logger.info("server sent state: {}", state);
    }

    private void processCommand(String commandText) {
        // Process the command and update the state
        logger.info("server received state: {}", commandText);
        try {
            final var commandValue = Double.parseDouble(commandText);
            // Update the state
            state += commandValue;
            logger.info("server updated state: {}", state);
        } catch (NumberFormatException e) {
            logger.warn("invalid command received: {}", commandText);
        }
    }
}
