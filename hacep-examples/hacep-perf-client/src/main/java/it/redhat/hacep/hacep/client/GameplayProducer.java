package it.redhat.hacep.hacep.client;

import it.redhat.hacep.rules.model.Gameplay;
import it.redhat.hacep.rules.model.util.GameplayBuilder;

import javax.jms.*;
import java.util.Random;

public class GameplayProducer {

    private final Connection connection;
    private final Session session;


    private String queueName;
    private long playerId;
    ConnectionFactory connectionFactory;

    public GameplayProducer(ConnectionFactory connectionFactory, String queueName, long playerId) {
        this.playerId = playerId;
        this.connectionFactory = connectionFactory;
        this.queueName = queueName;
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void produce(Integer id, Long timestamp) {
        try {
            Queue destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            Gameplay fact = new GameplayBuilder()
                    .id(id)
                    .playerId(playerId)
                    .timestamp(timestamp)
                    .build();
            System.out.print(".");
            ObjectMessage message = session.createObjectMessage(fact);
            message.setStringProperty("JMSXGroupID", String.format("P%05d", playerId));
            message.setIntProperty("JMSXGroupSeq", id);
            producer.send(message);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

}