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
import it.redhat.hacep.camel.UpgradeCommandRoute;
import it.redhat.hacep.command.model.Command;
import it.redhat.hacep.command.model.KeyValueParam;
import it.redhat.hacep.command.model.ResponseCode;
import it.redhat.hacep.command.model.ResponseMessage;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;
import static org.apache.camel.builder.PredicateBuilder.isEqualTo;
import static org.apache.camel.builder.PredicateBuilder.isInstanceOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class UpgradeCommandTest extends CamelTestSupport {

    private HACEP hacep = mock(HACEP.class);

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new UpgradeCommandRoute(hacep);
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testInputCommand() throws Exception {
        reset(hacep);

        String expectedReleaseID = "groupId:artifactId:version";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpointsAndSkip("direct:marshal-response");
                        replaceFromWith("direct:test");
                    }
                }
        );
        KeyValueParam param1 = new KeyValueParam();
        param1.setKey("RELEASE_ID");
        param1.setValue(expectedReleaseID);

        Command command = new Command();
        command.setCommand("UPGRADE");
        command.setParams(Arrays.asList(param1));

        when(hacep.update(anyString())).thenReturn(expectedReleaseID);

        context.start();

        getMockEndpoint("mock:direct:marshal-response").expectedMessageCount(1);
        getMockEndpoint("mock:direct:marshal-response")
                .message(0)
                .predicate(isInstanceOf(body(), ResponseMessage.class))
                .predicate(isEqualTo(simple("${body.code}"), constant(ResponseCode.SUCCESS)));

        Object object = template.requestBody("direct:test", command);

        verify(hacep, times(1)).update(eq(expectedReleaseID));
        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }

    @Test
    public void testExceptionOnInputCommand() throws Exception {
        reset(hacep);

        String expectedReleaseID = "groupId:artifactId:version";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpointsAndSkip("direct:marshal-response");
                        replaceFromWith("direct:test");
                    }
                }
        );
        KeyValueParam param1 = new KeyValueParam();
        param1.setKey("RELEASE_ID");
        param1.setValue(expectedReleaseID);

        Command command = new Command();
        command.setCommand("UPGRADE");
        command.setParams(Arrays.asList(param1));

        when(hacep.update(anyString())).thenThrow(new IllegalStateException("CANNOT UPDATE"));

        context.start();

        getMockEndpoint("mock:direct:marshal-response").expectedMessageCount(1);
        getMockEndpoint("mock:direct:marshal-response")
                .message(0)
                .predicate(isInstanceOf(body(), ResponseMessage.class))
                .predicate(isEqualTo(simple("${body.code}"), constant(ResponseCode.ERROR)));

        Object object = template.requestBody("direct:test", command);

        verify(hacep, times(1)).update(eq(expectedReleaseID));
        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }


    @Test
    public void testInvalidInputCommand() throws Exception {
        String expectedReleaseID = "groupId:artifactId:version";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpointsAndSkip("direct:marshal-response");
                        replaceFromWith("direct:test");
                    }
                }
        );
        KeyValueParam param1 = new KeyValueParam();
        param1.setKey("XRELEASE_ID");
        param1.setValue(expectedReleaseID);

        Command command = new Command();
        command.setCommand("UPGRADE");
        command.setParams(Arrays.asList(param1));

        context.start();

        getMockEndpoint("mock:direct:marshal-response").expectedMessageCount(1);
        getMockEndpoint("mock:direct:marshal-response")
                .message(0)
                .predicate(isInstanceOf(body(), ResponseMessage.class))
                .predicate(isEqualTo(simple("${body.code}"), constant(ResponseCode.ERROR)));

        Object object = template.requestBody("direct:test", command);

        assertMockEndpointsSatisfied(1, TimeUnit.MINUTES);
    }

}
