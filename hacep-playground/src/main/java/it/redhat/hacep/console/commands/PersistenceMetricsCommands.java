package it.redhat.hacep.console.commands;

import it.redhat.hacep.model.Key;
import it.redhat.hacep.cache.PlayerKey;
import it.redhat.hacep.console.UI;
import it.redhat.hacep.console.support.IllegalParametersException;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.rules.model.Gameplay;
import it.redhat.hacep.rules.model.util.GameplayGenerator;
import org.infinispan.Cache;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class PersistenceMetricsCommands implements ConsoleCommand {
    private static final String COMMAND_NAME = "meter";
    private final Cache<Key, Fact> cache;

    public PersistenceMetricsCommands(Cache<Key, Fact> cache) {
        this.cache = cache;
    }

    @Override
    public String command() {
        return COMMAND_NAME;
    }

    @Override
    public boolean execute(UI console, Iterator<String> args) throws IllegalParametersException {

        try {

            int ppid = Integer.parseInt(args.next());
            int numberOfDays = Integer.parseInt(args.next());
            int numberOfFacts = Integer.parseInt(args.next());
            int startingFrom = 0;

            if (args.hasNext()) {
                startingFrom = Integer.parseInt(args.next());
            }

            GameplayGenerator generator = new GameplayGenerator();

            generator.ppid(Long.valueOf(ppid)).gameCode("code").timestamp(System.currentTimeMillis(), numberOfDays, TimeUnit.DAYS).count(numberOfFacts + startingFrom);
            int skipCount = 0;
            for (Gameplay g : generator.generate()) {
                if (skipCount++ > startingFrom) {
                    Key key = new PlayerKey(String.valueOf(g.getId()), "" + ppid);
                    cache.put(key, g);
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalParametersException("Expected usage: meter <ppid> <numberOfDays> <numberOfFacts> [starting-from]. Example:\n meter 100 5 200 50\nWill produce 150 facts starting from 50 for user 100 distributed on 5 days");
        } catch (NoSuchElementException e) {
            throw new IllegalParametersException("Expected usage: meter <ppid> <numberOfDays> <numberOfFacts> [starting-from].");
        }
        return true;
    }

    @Override
    public void usage(UI console) {
        console.println(COMMAND_NAME + " <ppid> <numberOfDays> <numberOfFacts> [starting-from]");
        console.println("\t\tStart a workload session to check performances.");
    }

}
