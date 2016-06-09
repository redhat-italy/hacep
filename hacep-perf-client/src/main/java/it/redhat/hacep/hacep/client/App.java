package it.redhat.hacep.hacep.client;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class App {

    private ScheduledExecutorService scheduler = null;

    public App() {
        scheduler = Executors.newScheduledThreadPool(getConcurrentPlayers());
    }

    public static void main(String[] args) throws Exception {
        new App().produce();
    }

    private void produce() throws InterruptedException {
        ActiveMQConnectionFactory activeMQConnectionFactory;
        if (getSecurity()) {
            activeMQConnectionFactory = new ActiveMQConnectionFactory(getUsername(), getPassword(), "tcp://" + getBrokerHost());
        } else {
            activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://" + getBrokerHost());
        }
        PooledConnectionFactory connectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);
        connectionFactory.setMaxConnections(getPoolSize());
        connectionFactory.setMaximumActiveSessionPerConnection(500);

        if (getPreload()) {
            GameplayProducer[] producer = new GameplayProducer[getConcurrentPlayers()];
            for (int i = 0; i < getConcurrentPlayers(); i++) {
                producer[i] = new GameplayProducer(connectionFactory, getQueueName(), i);
            }

            ZonedDateTime time = ZonedDateTime.now().minusSeconds(getEventInterval() * (getPreloadMessage() + 10));
            for (int msg = 1; msg <= getPreloadMessage(); msg++) {
                for (int i = 0; i < getConcurrentPlayers(); i++) {
                    producer[i].produce(msg, time.toInstant().toEpochMilli());
                }
                time = time.plusSeconds(getEventInterval());
            }
        }


        for (int i = 0; i < getConcurrentPlayers(); i++) {
            int delay = (int) (Math.random() * getDelayRange());
            final ScheduledFuture<?> playerHandle = scheduler
                    .scheduleAtFixedRate(
                            new MyRunner(new GameplayProducer(connectionFactory, getQueueName(), i)),
                            delay,
                            getEventInterval(),
                            TimeUnit.SECONDS);
            scheduler.schedule(() -> playerHandle.cancel(true), getDuration(), TimeUnit.MINUTES);
        }
    }

    private int getPoolSize() {
        try {
            return Integer.valueOf(System.getProperty("pool.size", "8"));
        } catch (IllegalArgumentException e) {
            return 8;
        }
    }

    private int getDuration() {
        try {
            return Integer.valueOf(System.getProperty("duration", "15"));
        } catch (IllegalArgumentException e) {
            return 15;
        }
    }

    private String getBrokerHost() {
        try {
            return System.getProperty("broker.host", "localhost:61616");
        } catch (IllegalArgumentException e) {
            return "localhost:61616";
        }
    }

    private boolean getPreload() {
        try {
            return Boolean.valueOf(System.getProperty("test.preload", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Integer getPreloadMessage() {
        try {
            return Integer.valueOf(System.getProperty("test.messages", "1000"));
        } catch (IllegalArgumentException e) {
            return 1000;
        }
    }

    private boolean getSecurity() {
        try {
            return Boolean.valueOf(System.getProperty("broker.security", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getUsername() {
        try {
            return System.getProperty("broker.usr", "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private String getPassword() {
        try {
            return System.getProperty("broker.pwd", "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private String getQueueName() {
        try {
            return System.getProperty("queue.name", "facts");
        } catch (IllegalArgumentException e) {
            return "facts";
        }
    }

    private int getConcurrentPlayers() {
        try {
            return Integer.valueOf(System.getProperty("concurrent.players", "5"));
        } catch (IllegalArgumentException e) {
            return 5;
        }
    }

    private int getDelayRange() {
        try {
            return Integer.valueOf(System.getProperty("delay.range", "15"));
        } catch (IllegalArgumentException e) {
            return 15;
        }
    }

    private int getEventInterval() {
        try {
            return Integer.valueOf(System.getProperty("event.interval", "3"));
        } catch (IllegalArgumentException e) {
            return 3;
        }
    }

    private class MyRunner implements Runnable {

        private final GameplayProducer gameplayProducer;
        private int id = 10000;

        public MyRunner(GameplayProducer gameplayProducer) {
            this.gameplayProducer = gameplayProducer;
        }

        @Override
        public void run() {
            gameplayProducer.produce(id++, System.currentTimeMillis());
        }
    }
}