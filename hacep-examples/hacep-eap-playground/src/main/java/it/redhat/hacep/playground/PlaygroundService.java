/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        try {
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
            return Response.ok(restUI.getContent()).build();
        } finally {
            restUI.clear();
        }
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
