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

package it.redhat.hacep.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.config.StreamResequencerConfig;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class ResequencerTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .resequence(header("seqnum")).stream(new StreamResequencerConfig(10000, 100))
                        .to("mock:result");
            }
        };
    }

    @Test
    public void testSequenceMessage() throws InterruptedException {
        MockEndpoint mockOut = getMockEndpoint("mock:result");
        mockOut.setExpectedMessageCount(4);
        mockOut.message(0).header("seqnum").isEqualTo(0);
        mockOut.message(1).header("seqnum").isEqualTo(2);
        mockOut.message(2).header("seqnum").isEqualTo(3);
        mockOut.message(3).header("seqnum").isEqualTo(10);

        template.sendBodyAndHeader("direct:start", "", "seqnum", 10);
        template.sendBodyAndHeader("direct:start", "", "seqnum", 3);
        template.sendBodyAndHeader("direct:start", "", "seqnum", 0);
        template.sendBodyAndHeader("direct:start", "", "seqnum", 2);

        assertMockEndpointsSatisfied();
    }

    @Test
    @Ignore
    public void testManySequenceMessage() throws InterruptedException {
        MockEndpoint mockOut = getMockEndpoint("mock:result");

        int totalExpected = 100000;
        mockOut.setExpectedMessageCount(totalExpected);

        for (int i=0; i<totalExpected; i++) {
            mockOut.message(i).header("seqnum").isEqualTo(i);
        }

        for (int i=totalExpected; i>0; i--) {
            template.sendBodyAndHeader("direct:start", "", "seqnum", totalExpected-i);
        }

        assertMockEndpointsSatisfied();
    }
}
