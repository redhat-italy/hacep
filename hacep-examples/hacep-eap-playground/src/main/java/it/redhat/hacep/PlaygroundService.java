package it.redhat.hacep;

import it.redhat.hacep.console.ReSTUI;
import it.redhat.hacep.console.commands.ConsoleCommand;
import it.redhat.hacep.console.commands.HelpConsoleCommand;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

@ApplicationPath("/")
@Path("/")
public class PlaygroundService extends Application {

    @Inject
    private ReSTUI restUI;

    @GET
    @Path("/execute/{command}")
    @Produces("application/json")
    public Response executeCommand(@PathParam("command") String command) {
        return Response.ok().build();
    }

    @GET
    @Path("/info/{command}")
    @Produces("application/json")
    public Response infoCommand(@Context UriInfo uriInfo, @PathParam("command") String commandName) {
        Optional<ConsoleCommand> command = restUI.findByName(commandName);
        command
                .orElseGet(HelpConsoleCommand::new)
                .usage(restUI);
        return Response.ok(restUI.toString()).build();
    }
}
