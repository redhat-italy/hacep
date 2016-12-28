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

package it.redhat.hacep.command.model;

import it.redhat.hacep.camel.ExecuteCommandsFromJmsRoute;
import it.redhat.hacep.command.ExecutableCommand;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.camel.builder.PredicateBuilder.*;

public class CommandDTOTest extends CamelTestSupport {

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new ExecuteCommandsFromJmsRoute("commands");
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }


    @Test
    public void testInputCommand() throws Exception {
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:test");
                        mockEndpoints("direct:execute-command");
                    }
                }
        );
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:NOME_COMMAND")
                        .to("mock:results");
            }
        });

        String input = "{\n" +
                "  \"command\": \"NOME_COMMAND\",\n" +
                "  \"params\": [\n" +
                "    {\n" +
                "      \"key\": \"NOME_CHIAVE1\",\n" +
                "      \"value\": \"VALORE1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"key\": \"NOME_CHIAVE2\",\n" +
                "      \"value\": \"VALORE2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        context.start();

        getMockEndpoint("mock:direct:execute-command", false).expectedMessageCount(1);
        getMockEndpoint("mock:direct:execute-command", false)
                .message(0)
                .predicate(isInstanceOf(body(), CommandDTO.class))
                .predicate(isEqualTo(Builder.simple("${body.command}"), Builder.constant("NOME_COMMAND")))
                .predicate(isEqualTo(Builder.simple("${body.params?.size}"), Builder.constant(2)))
                .predicate(isInstanceOf(Builder.simple("${body.params[0]}"), KeyValueParam.class))
                .predicate(isEqualTo(Builder.simple("${body.params[0].key}"), Builder.constant("NOME_CHIAVE1")))
                .predicate(isEqualTo(Builder.simple("${body.params[0].value}"), Builder.constant("VALORE1")))
                .predicate(isInstanceOf(Builder.simple("${body.params[1]}"), KeyValueParam.class))
                .predicate(isEqualTo(Builder.simple("${body.params[1].key}"), Builder.constant("NOME_CHIAVE2")))
                .predicate(isEqualTo(Builder.simple("${body.params[1].value}"), Builder.constant("VALORE2")));

        getMockEndpoint("mock:results", false).expectedMessageCount(1);

        sendBody("direct:test", input);

        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }
}
