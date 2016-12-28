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

import it.redhat.hacep.command.model.CommandDTO;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class ExecuteCommandsFromJmsRoute extends RouteBuilder {

    private String queueName;

    public ExecuteCommandsFromJmsRoute(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void configure() throws Exception {
        from("jms:" + queueName)
                .setExchangePattern(ExchangePattern.InOut)
                .unmarshal().json(JsonLibrary.Jackson, CommandDTO.class)
                .to("direct:execute-command");

        from("direct:execute-command")
                .setExchangePattern(ExchangePattern.InOut)
                .recipientList()
                .simple("direct:${body.command}");
    }
}
