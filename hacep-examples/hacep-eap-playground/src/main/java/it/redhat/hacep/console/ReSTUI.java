package it.redhat.hacep.console;

import it.redhat.hacep.console.commands.ConsoleCommand;
import it.redhat.hacep.console.support.ConsoleCommandComparator;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class ReSTUI implements UI {

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
        out.println("Commands:");
        StreamSupport.stream(commands.spliterator(), true)
                .sorted(new ConsoleCommandComparator())
                .forEachOrdered(c -> c.usage(this));
    }

    @Override
    public String toString() {
        out.flush();
        return os.toString();
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
