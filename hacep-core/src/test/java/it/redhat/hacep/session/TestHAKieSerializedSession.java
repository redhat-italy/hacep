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

package it.redhat.hacep.session;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cluster.AbstractClusterTest;
import it.redhat.hacep.cluster.RulesConfigurationTestImpl;
import it.redhat.hacep.configuration.RulesManager;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.runtime.Channel;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestHAKieSerializedSession extends AbstractClusterTest implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestHAKieSerializedSession.class);

    private static ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Mock
    private Channel replayChannel;
    @Mock
    private Channel additionsChannel;

    @Test
    public void serialization() throws IOException, ClassNotFoundException, InterruptedException {
        LOGGER.info("Start test TestHAKieSerializedSession serialization");
        System.setProperty("grid.buffer", "10");

        RulesConfigurationTestImpl rulesConfigurationTest = RulesConfigurationTestImpl.RulesTestBuilder.buildV1();
        rulesConfigurationTest.registerChannel("additions", additionsChannel, replayChannel);

        RulesManager rulesManager = new RulesManager(rulesConfigurationTest);
        rulesManager.start(null, null, null);

        Cache<String, Object> cache1 = startNodes(1, rulesManager).getCache();

        StreamingMarshaller marshaller = cache1.getAdvancedCache().getComponentRegistry().getCacheMarshaller();

        HAKieSerializedSession hakss = new HAKieSerializedSession( rulesManager, executorService );
        org.infinispan.commons.io.ByteBuffer bb = marshaller.objectToBuffer( hakss );
        HAKieSerializedSession hakss2 = (HAKieSerializedSession)marshaller.objectFromByteBuffer( bb.getBuf() );
        Assert.assertEquals( hakss.getSessionSize(), hakss2.getSessionSize() );
        Assert.assertEquals( hakss.getSerializedSession(), hakss2.getSerializedSession() );

        hakss.add(new Fact() {
            @Override
            public Instant getInstant() {
                return null;
            }

            @Override
            public Key extractKey() {
                return null;
            }
        });
        bb = marshaller.objectToBuffer( hakss );
        hakss2 = (HAKieSerializedSession)marshaller.objectFromByteBuffer( bb.getBuf() );
        Assert.assertEquals( hakss.getSessionSize(), hakss2.getSessionSize() );
        Assert.assertEquals( hakss.getSerializedSession(), hakss2.getSerializedSession() );

        LOGGER.info("Stop test TestHAKieSerializedSession serialization");
    }

    @Override
    protected Channel getReplayChannel() {
        return replayChannel;
    }
}
