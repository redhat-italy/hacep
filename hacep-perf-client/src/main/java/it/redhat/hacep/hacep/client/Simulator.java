package it.redhat.hacep.hacep.client;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Simulator {

    private final static long startTime = new Date().getTime();

    public static void main(String[] args) throws Exception {
        new Simulator().execute();
    }

    private void execute() throws InterruptedException {
        if (isPreconfigured()){
            thirtyDaysRun();
        } else {
            produce();
        }
    }

    private void thirtyDaysRun() throws InterruptedException {
        for (int i=0; i<30; i++){
            for (int j=0; j<23; j++){
                oneHourRun(i, j);
            }
        }
    }

    private void oneHourRun(int dayOfRun, int hourOfRun) {
        System.out.println("==================== Simulating day " + dayOfRun + " hour " + hourOfRun + "\n");
        long time = startTime + TimeUnit.DAYS.toMillis(dayOfRun) + TimeUnit.HOURS.toMillis(hourOfRun);
        int count=0;
        if ((hourOfRun == 20) || (hourOfRun == 21)){
            count = 150;
        }
        if ((hourOfRun == 18) || (hourOfRun == 19) || (hourOfRun == 22) || (hourOfRun == 23)){
            count = 100;
        }
        if (((hourOfRun >= 0) && (hourOfRun <= 3)) || (hourOfRun == 16) || (hourOfRun == 17)){
            count = 50;
        }
        if (((hourOfRun >= 4) && (hourOfRun <= 7)) || ((hourOfRun >= 12) && (hourOfRun == 15))){
            count = 25;
        }
        if (count>0){
            for (int i=getStartingId(); i<(getStartingId()+getConcurrentPlayers()); i++){
                PlayerSimulator player = new PlayerSimulator(i)
                        .setBrokerUrl(getBrokerHost())
                        .setQueueName(getQueueName())
                        .setNumberOfFacts(count)
                        .setStartTimel(time)
                        .setInterval(3600 / count);
                if (getSecurity()){
                    player.setSecurityOn(getUsername(), getPassword());
                }
                Thread thread = new Thread(player);
                thread.setDaemon(false);
                thread.start();
            }
        }
        try {
            Thread.currentThread().sleep(TimeUnit.SECONDS.toMillis(15));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void produce() throws InterruptedException {
        System.out.println("==================== Starting simulator ====================\n");
        for (int i=getStartingId(); i<(getStartingId()+getConcurrentPlayers()); i++){
            PlayerSimulator player = new PlayerSimulator(i).setBrokerUrl(getBrokerHost()).setQueueName(getQueueName()).setNumberOfFacts(getNumberOfFacts());
            if (getSecurity()){
                player.setSecurityOn(getUsername(), getPassword());
            }
            Thread thread = new Thread(player);
            thread.setDaemon(false);
            thread.start();
        }
    }

    private boolean isPreconfigured() {
        try {
            return Boolean.valueOf(System.getProperty("preconfigured", "false"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getBrokerHost() {
        try {
            return System.getProperty("broker.host", "localhost:61616");
        } catch (IllegalArgumentException e) {
            return "localhost:61616";
        }
    }

    private Integer getNumberOfFacts() {
        try {
            return Integer.valueOf(System.getProperty("facts.number", "10"));
        } catch (IllegalArgumentException e) {
            return 10;
        }
    }

    private Integer getStartingId() {
        try {
            return Integer.valueOf(System.getProperty("start.id", "0"));
        } catch (IllegalArgumentException e) {
            return 0;
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