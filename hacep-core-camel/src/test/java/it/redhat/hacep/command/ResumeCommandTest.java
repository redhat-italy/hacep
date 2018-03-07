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

package it.redhat.hacep.command;

import it.redhat.hacep.HACEP;
import it.redhat.hacep.camel.ResumeCommandRoute;
import it.redhat.hacep.command.model.Command;
import it.redhat.hacep.command.model.ResponseCode;
import it.redhat.hacep.command.model.ResponseMessage;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;
import static org.apache.camel.builder.PredicateBuilder.isEqualTo;
import static org.apache.camel.builder.PredicateBuilder.isInstanceOf;
import static org.mockito.Mockito.*;

public class ResumeCommandTest extends CamelTestSupport {

    private HACEP hacep = mock(HACEP.class);

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new ResumeCommandRoute(hacep);
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testInputCommand() throws Exception {
        reset(hacep);

        hacep.suspend();
        
        String expectedOutput = "OK";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpointsAndSkip("direct:marshal-response");
                        replaceFromWith("direct:test");
                    }
                }
        );

        Command command = new Command();
        command.setCommand("RESUME");

        context.start();

        getMockEndpoint("mock:direct:marshal-response").expectedMessageCount(1);
        getMockEndpoint("mock:direct:marshal-response")
                .message(0)
                .predicate(isInstanceOf(body(), ResponseMessage.class))
                .predicate(isEqualTo(simple("${body.code}"), constant(ResponseCode.SUCCESS)))
                .predicate(isEqualTo(simple("${body.message}"), constant(expectedOutput)));

        Object object = template.requestBody("direct:test", command);

        verify(hacep, times(1)).resume();
        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }

    @Test
    public void testExceptionOnInputCommand() throws Exception {
        reset(hacep);

        hacep.suspend();
        
        String expectedOutput = "OK";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpointsAndSkip("direct:marshal-response");
                        replaceFromWith("direct:test");
                    }
                }
        );

        Command command = new Command();
        command.setCommand("RESUME");

        doThrow(new RuntimeException("CANNOT SUSPEND ROUTE")).when(hacep).resume();
        context.start();

        getMockEndpoint("mock:direct:marshal-response").expectedMessageCount(1);
        getMockEndpoint("mock:direct:marshal-response")
                .message(0)
                .predicate(isInstanceOf(body(), ResponseMessage.class))
                .predicate(isEqualTo(simple("${body.code}"), constant(ResponseCode.ERROR)));
        
        Object object = template.requestBody("direct:test", command);
        
        verify(hacep, times(1)).resume();

        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }

}
