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

import it.redhat.hacep.cache.RulesUpdateVersion;
import it.redhat.hacep.camel.UpgradeCommandRoute;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class UpgradeCommandTest extends CamelTestSupport {

    private RulesUpdateVersion rulesUpdateVersion = mock(RulesUpdateVersion.class);

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new UpgradeCommandRoute(rulesUpdateVersion);
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testInputCommand() throws Exception {
        String expectedReleaseID = "groupId:artifactId:version";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:test");
                    }
                }
        );
        KeyValueParam param1 = new KeyValueParam();
        param1.setKey("RELEASE_ID");
        param1.setValue(expectedReleaseID);

        CommandDTO commandDTO = new CommandDTO();
        commandDTO.setCommand("UPGRADE");
        commandDTO.setParams(Arrays.asList(param1));

        context.start();

        reset(rulesUpdateVersion);

        sendBody("direct:test", commandDTO);

        verify(rulesUpdateVersion, times(1)).execute(eq(expectedReleaseID));
    }

    @Test(expected = CamelExecutionException.class)
    public void testInvalidInputCommand() throws Exception {
        String expectedReleaseID = "groupId:artifactId:version";

        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:test");
                    }
                }
        );
        KeyValueParam param1 = new KeyValueParam();
        param1.setKey("XRELEASE_ID");
        param1.setValue(expectedReleaseID);

        CommandDTO commandDTO = new CommandDTO();
        commandDTO.setCommand("UPGRADE");
        commandDTO.setParams(Arrays.asList(param1));

        context.start();

        template.requestBody("direct:test", commandDTO);
    }
}
