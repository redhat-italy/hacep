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

package it.redhat.hacep.cache.session;

import it.redhat.hacep.cache.session.utils.SessionUtils;
import it.redhat.hacep.configuration.DroolsConfiguration;
import it.redhat.hacep.model.Fact;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinispan.commons.util.Util.asSet;

public class HASession implements DeltaAware {

    private final static Logger logger = LoggerFactory.getLogger(HASession.class);

    private final DroolsConfiguration droolsConfiguration;

    private KieSession session;
    private Queue<Fact> buffer = new ConcurrentLinkedQueue<>();

    public HASession(DroolsConfiguration droolsConfiguration) {
        this.droolsConfiguration = droolsConfiguration;
    }

    public HASession(DroolsConfiguration droolsConfiguration, KieSession session) {
        this.droolsConfiguration = droolsConfiguration;
        this.session = session;
    }

    public void insert(Fact fact) {
        if (session == null) {
            session = droolsConfiguration.getKieSession();
            droolsConfiguration.getChannels().forEach(session::registerChannel);
        }
        buffer.add(fact);
        SessionUtils.advanceClock(session, fact);
        session.insert(fact);
        session.fireAllRules();
    }

    @Override
    public Delta delta() {
        Fact fact = buffer.poll();
        if (fact != null) {
            return new HASessionDeltaFact(fact);
        }
        return new HASessionDeltaEmpty();
    }

    @Override
    public void commit() {
        buffer.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HASession)) return false;

        HASession haSession = (HASession) o;

        return (session != null ? session.equals(haSession.session) : haSession.session == null) &&
                this.buffer.stream().allMatch(haSession.buffer::contains) &&
                haSession.buffer.stream().allMatch(this.buffer::contains);
    }

    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + buffer.hashCode();
        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        if (session != null) {
            SessionUtils.dispose(session);
            session = null;
        }
        super.finalize();
    }

    public static class HASessionExternalizer implements AdvancedExternalizer<HASession> {

        private final DroolsConfiguration droolsConfiguration;

        public HASessionExternalizer(DroolsConfiguration droolsConfiguration) {
            this.droolsConfiguration = droolsConfiguration;
        }

        @Override
        public Set<Class<? extends HASession>> getTypeClasses() {
            return asSet(HASession.class);
        }

        @Override
        public Integer getId() {
            return JDGExternalizerIDs.HASessionID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HASession object) throws IOException {
            output.writeObject(object.session);
            object.buffer.clear();
        }

        @Override
        public HASession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            HASession haSession = new HASession(droolsConfiguration);
            haSession.session = (KieSession) input.readObject();
            droolsConfiguration.getChannels().forEach(haSession.session::registerChannel);
            return haSession;
        }
    }
}
