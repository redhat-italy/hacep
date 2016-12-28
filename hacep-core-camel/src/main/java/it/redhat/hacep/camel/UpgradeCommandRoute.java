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

import it.redhat.hacep.cache.RulesUpdateVersion;
import it.redhat.hacep.command.model.CommandDTO;
import it.redhat.hacep.command.model.KeyValueParam;
import org.apache.camel.builder.RouteBuilder;

public class UpgradeCommandRoute extends RouteBuilder {

    private RulesUpdateVersion ruleBean;

    public UpgradeCommandRoute(RulesUpdateVersion ruleBean) {
        this.ruleBean = ruleBean;
    }

    @Override
    public void configure() throws Exception {
        from("direct:UPGRADE")
                .process(exchange -> {
                    CommandDTO dto = exchange.getIn().getBody(CommandDTO.class);
                    String value = dto.getParams().stream()
                            .filter(kv -> (kv.getKey() != null && kv.getKey().equals("RELEASE_ID")))
                            .map(KeyValueParam::getValue)
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("RELEASE_ID cannot be null"));
                    exchange.getOut().setBody(value);
                })
                .bean(ruleBean, "execute(${body})");
    }
}
