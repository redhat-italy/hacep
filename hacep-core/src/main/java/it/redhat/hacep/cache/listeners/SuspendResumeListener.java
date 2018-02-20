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

import it.redhat.hacep.configuration.Router;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = false, sync = true, observation = Listener.Observation.POST)
public class SuspendResumeListener {

    private final Logger LOGGER = LoggerFactory.getLogger(SuspendResumeListener.class);
    private final Router router;


    public SuspendResumeListener(Router router) {
        this.router = router;
    }

    @CacheEntryModified
    public synchronized void eventReceived(CacheEntryModifiedEvent event) {
        Object key = event.getKey();
        Object value = event.getValue();

        if (LOGGER.isDebugEnabled()) LOGGER.debug("Received Suspend Event key=[{}] value=[{}]", key, value);

        if (Router.SUSPEND.equals(key)) {
            router.suspend();
        }

        if (Router.RESUME.equals(key)) {
            router.resume();
        }
    }


}
