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

package it.redhat.hacep.cache.listeners;

import it.redhat.hacep.cache.session.KieSessionSaver;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = true, observation = Listener.Observation.POST)
public class FactListenerPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(KieSessionSaver.class);

    private final KieSessionSaver saver;

    public FactListenerPost(KieSessionSaver kieSessionSaver) {
        this.saver = kieSessionSaver;
    }

    @CacheEntryCreated
    public void eventReceived(CacheEntryCreatedEvent event) {
        Object key = event.getKey();
        Object value = event.getValue();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Event received: (" + key + ", " + value + ")");

        if (isAnHACEPEvent(key, value)) {
            LOGGER.warn("Event is not HACEP compliant: (" + key + ", " + value + ")");
            return;
        }
        saver.insert((Key) key, (Fact) value);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Chain complete for: (" + key + ", " + value + ")");

    }

    private boolean isAnHACEPEvent(Object key, Object value) {
        return !(checkClass(Fact.class, value) && checkClass(Key.class, key));
    }

    private boolean checkClass(Class<?> clazz, Object o) {
        return (o != null && clazz.isAssignableFrom(o.getClass()));
    }

}