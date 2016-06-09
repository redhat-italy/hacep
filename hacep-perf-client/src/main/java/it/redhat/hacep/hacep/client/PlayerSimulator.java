package it.redhat.hacep.hacep.client;

import it.redhat.hacep.rules.model.Gameplay;
import it.redhat.hacep.rules.model.util.GameplayBuilder;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Date;
import java.util.Random;

public class PlayerSimulator implements Runnable {
    private long ppid = new Random().nextLong();
    private int numberOfFacts = 10;
    private int eventInterval = 3;
    private long startTime = new Date().getTime();
    private String brokerUrl = "localhost:61616";
    private String queueName = "facts";
    private boolean security = false;
    private String usr = "admin";
    private String pwd = "admin";
    private String[] codes = new String[]{"code", "test", "play", "other", "win", "bet", "reward", "random", "something", "event"};

    public PlayerSimulator(long ppid){
        this.ppid = ppid;
    }

    public PlayerSimulator setNumberOfFacts(int size){
        this.numberOfFacts = size;
        return this;
    }

    public PlayerSimulator setStartTimel(long startTime){
        this.startTime = startTime;
        return this;
    }

    public PlayerSimulator setInterval(int delay){
        this.eventInterval = delay;
        return this;
    }

    public PlayerSimulator setBrokerUrl(String url){
        this.brokerUrl = url;
        return this;
    }

    public PlayerSimulator setQueueName(String queueName){
        this.queueName = queueName;
        return this;
    }

    public PlayerSimulator setSecurityOn(String usr, String pwd){
        this.security = true;
        this.usr = usr;
        this.pwd = pwd;
        return this;
    }

    public void run() {
        Random base = new Random();

        try {
            ActiveMQConnectionFactory connectionFactory = null;
            if (security) {
                connectionFactory = new ActiveMQConnectionFactory(usr, pwd, "tcp://" + brokerUrl);
            } else {
                connectionFactory = new ActiveMQConnectionFactory("tcp://" + brokerUrl);
            }
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            for (int i=0; i<numberOfFacts; i++){
                Gameplay fact = new GameplayBuilder(ppid)
                        .id(base.nextLong())
                        .gameCode(getCode())
                        .timestamp(startTime + (i * eventInterval))
                        .build();
                ObjectMessage message = session.createObjectMessage();
                message.setObject(fact);
                producer.send(message);
                System.out.print(".");
            }
            session.close();
            connection.close();
            System.out.println("\n======================= Player id: " + ppid + " preloaded " + numberOfFacts + " facts.");

        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }

    private String getCode() {
        int codePointer = (int) (Math.random() * codes.length);
        return codes[codePointer];
    }

}