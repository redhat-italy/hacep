package it.redhat.hacep.playground.event.send;

import it.redhat.hacep.model.Fact;

import javax.jms.*;

public class Sender {

    private final Session session;
    private String queueName;

    public Sender(ConnectionFactory connectionFactory, String queueName) {
        this.queueName = queueName;
        try {
            Connection connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Fact fact) {
        try {
            Queue destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            ObjectMessage message = session.createObjectMessage(fact);
            //message.setStringProperty("JMSXGroupID", String.format("P%05d", playerId));
            producer.send(message);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

}