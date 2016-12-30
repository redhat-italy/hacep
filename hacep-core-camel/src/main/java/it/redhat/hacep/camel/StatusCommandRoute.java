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

import it.redhat.hacep.HACEP;
import it.redhat.hacep.command.model.ResponseCode;
import it.redhat.hacep.command.model.ResponseMessage;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;

public class StatusCommandRoute extends RouteBuilder {

    private final HACEP hacep;

    public StatusCommandRoute(HACEP hacep) {
        this.hacep = hacep;
    }

    @Override
    public void configure() throws Exception {
        from("direct:STATUS")
                .onException(Exception.class)
                    .maximumRedeliveries(0)
                    .handled(true)
                    .process(exchange -> {
                        Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
                        exchange.getOut().setBody(new ResponseMessage(ResponseCode.ERROR, exception.getMessage()));
                    })
                    .to("direct:marshal-response")
                .end()
                .setExchangePattern(ExchangePattern.InOut)
                .bean(hacep, "status()", false)
                .process(exchange -> {
                    Object body = exchange.getIn().getBody();
                    ResponseMessage output = new ResponseMessage(ResponseCode.SUCCESS, (String) body);
                    exchange.getOut().setBody(output);
                })
                .to("direct:marshal-response");
    }
}
