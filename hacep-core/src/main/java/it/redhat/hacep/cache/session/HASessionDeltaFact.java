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

public class HASessionDeltaFact implements Delta {

    private final static Logger logger = LoggerFactory.getLogger(HASessionDeltaFact.class);

    private final Fact fact;

    public HASessionDeltaFact(Fact fact) {
        this.fact = fact;
    }

    @Override
    public DeltaAware merge(DeltaAware d) {
        if (d == null) {
            throw new IllegalStateException();
        }
        HASerializedSession haSession = (HASerializedSession) d;
        haSession.add(fact);
        return haSession;
    }

    public Fact getFact() {
        return fact;
    }

    public static class HASessionDeltaFactExternalizer implements AdvancedExternalizer<HASessionDeltaFact> {

        @Override
        public Set<Class<? extends HASessionDeltaFact>> getTypeClasses() {
            return Util.asSet(HASessionDeltaFact.class);
        }

        @Override
        public Integer getId() {
            return JDGExternalizerIDs.HASessionDeltaFactID.getId();
        }

        @Override
        public void writeObject(ObjectOutput output, HASessionDeltaFact object) throws IOException {
            output.writeObject(object.getFact());
        }

        @Override
        public HASessionDeltaFact readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Object o = input.readObject();
            return new HASessionDeltaFact((Fact) o);
        }
    }
}
