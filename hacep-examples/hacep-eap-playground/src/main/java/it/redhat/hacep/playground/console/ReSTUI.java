package it.redhat.hacep.playground.console;

import it.redhat.hacep.playground.console.commands.ConsoleCommand;
import it.redhat.hacep.playground.console.support.ConsoleCommandComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class ReSTUI implements UI {

    private final static Logger log = LoggerFactory.getLogger(ReSTUI.class);

    @Inject
    private Instance<ConsoleCommand> commands;

    private ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
    private PrintWriter out = new PrintWriter(os);

    @Override
    public void print(Object message) {
        out.print(message);
    }

    @Override
    public void println(Object message) {
        out.println(message);
    }

    @Override
    public void print(String message) {
        out.print(message);
    }

    @Override
    public void println(String message) {
        out.print(message);
    }

    @Override
    public void println() {
        out.println();
    }

    @Override
    public void printUsage() {
        log.info("Start print usage");
        StreamSupport.stream(commands.spliterator(), true)
                .sorted(new ConsoleCommandComparator())
                .forEachOrdered(c -> {
                    c.usage(this);
                    out.println();out.println();
                });
    }

    @Override
    public String toString() {
        try {
            out.flush();
            return os.toString();
        } finally {
            os.reset();
        }
    }

    @Override
    public void start() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public UI register(ConsoleCommand cmd) {
        throw new IllegalStateException();
    }

    public Optional<ConsoleCommand> findByName(String name) {
        return StreamSupport.stream(commands.spliterator(), false)
                .filter(c -> name.equalsIgnoreCase(c.command()))
                .findFirst();
    }

}
