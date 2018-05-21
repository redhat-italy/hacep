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

import it.redhat.hacep.model.Fact;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class HAKieSessionDeltaFact implements Delta {

    private final static Logger LOGGER = LoggerFactory.getLogger(HAKieSessionDeltaFact.class);

    private final HAKieSessionBuilder builder;

    private final Fact fact;

    public HAKieSessionDeltaFact(HAKieSessionBuilder builder, Fact fact) {
        this.builder = builder;
        this.fact = fact;
    }

    @Override
    public DeltaAware merge(DeltaAware d) {
        if (d == null) {
            LOGGER.debug("[HAKieSessionDeltaFact: {}, FactKey {}, FactInstant: {}], merged with null, this should only happen during key remove", fact, fact == null ? null : fact.extractKey(), fact == null ? null : fact.getInstant());
            HAKieSerializedSession haSession = builder.buildSerialized();
            haSession.add(fact);
//            throw new IllegalStateException();
            return haSession;
        }

        HAKieSerializedSession haSession;
        if (HAKieSerializedSession.class.isAssignableFrom(d.getClass())) {
            haSession = (HAKieSerializedSession) d;
        } else {
            if (HAKieSession.class.isAssignableFrom(d.getClass())) {
                haSession = ((HAKieSession) d).wrapWithSerializedSession();
            } else {
                //XXX: This should never happen
                throw new IllegalArgumentException("Class [" + d.getClass() + "]");
            }
        }
        haSession.add(fact);
        return haSession;
    }

    public Fact getFact() {
        return fact;
    }

    public static class HASessionDeltaFactExternalizer implements AdvancedExternalizer<HAKieSessionDeltaFact> {

        private final HAKieSessionBuilder builder;

        public HASessionDeltaFactExternalizer(HAKieSessionBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Set<Class<? extends HAKieSessionDeltaFact>> getTypeClasses() {
            return Util.asSet(HAKieSessionDeltaFact.class);
        }

        @Override
        public Integer getId() {
            return JDGExternalizerIDs.HASessionDeltaFactID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HAKieSessionDeltaFact object) throws IOException {
            output.writeObject(object.getFact());
        }

        @Override
        public HAKieSessionDeltaFact readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Object o = input.readObject();
            return new HAKieSessionDeltaFact(builder, (Fact) o);
        }
    }
}
