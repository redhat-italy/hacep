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

package it.redhat.hacep.cluster;

import it.redhat.hacep.cache.listeners.UpdateVersionListener;
import it.redhat.hacep.configuration.Router;
import it.redhat.hacep.configuration.RulesManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = false, sync = true, observation = Listener.Observation.POST)
public class UpdateVersionListnerError {

    private final Logger LOGGER = LoggerFactory.getLogger(UpdateVersionListener.class);

    private final Router router;
    private final RulesManager rulesManager;

    public UpdateVersionListnerError(Router router, RulesManager rulesManager) {
        this.router = router;
        this.rulesManager = rulesManager;
    }

    @CacheEntryModified
    public synchronized void eventReceived(CacheEntryModifiedEvent event) {
        Object key = event.getKey();
        Object value = event.getValue();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Received MODIFIED key on INFOS key=[{}] value=[{}]", key, value);
        if (RulesManager.RULES_ARTIFACT_ID.equals(key) || RulesManager.RULES_GROUP_ID.equals(key)) {
            throw new IllegalStateException("Cannot change rules artifact or group id.");
        }
        if ( RulesManager.RULES_VERSION.equals(key) && !value.equals("1.0.0") ) {
            throw new RuntimeException( "Upgrade rules version failed!!!" );
        } else if ( RulesManager.RULES_VERSION.equals(key) ){
            updateVersion((String) value);
        }
    }

    private void updateVersion(String value) {
        try {
            router.suspend();
            rulesManager.updateToVersion(value);
        } finally {
            router.resume();
        }
    }

}
