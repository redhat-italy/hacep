package it.redhat.hacep.playground;

import it.redhat.hacep.playground.console.ReSTUI;
import it.redhat.hacep.playground.console.commands.ConsoleCommand;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@ApplicationPath("/")
@Path("/")
public class PlaygroundService extends Application {

    @Inject
    private ReSTUI restUI;

    @GET
    @Path("/execute/{command}")
    @Produces("application/json")
    public Response executeCommand(@PathParam("command") String commandName, @QueryParam("params") String params) {
        Optional<ConsoleCommand> command = restUI.findByName(commandName);
        if (command.isPresent()) {
            if (params != null) {
                command.get().execute(restUI, Arrays.asList(params.split(",")).iterator());
            } else {
                command.get().execute(restUI, Collections.emptyIterator());
            }
        } else {
            restUI.printUsage();
        }
        return Response.ok(restUI.toString()).build();
    }

    @GET
    @Path("/help")
    @Produces("application/json")
    public Response help() {
        restUI.printUsage();
        return Response.ok(restUI.toString()).build();
    }

    @GET
    @Path("/help/{command}")
    @Produces("application/json")
    public Response helpOnCommand(@PathParam("command") String commandName) {
        Optional<ConsoleCommand> command = restUI.findByName(commandName);
        if (command.isPresent()) {
            command.get().usage(restUI);
            return Response.ok(restUI.toString()).build();
        }
        return help();
    }
}
