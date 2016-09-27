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

package it.redhat.hacep.distributed;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.configuration.annotations.HACEPSessionCache;
import it.redhat.hacep.model.Key;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class Snapshotter implements Callable<Boolean>, Serializable {

    @Inject
    @HACEPSessionCache
    private Cache<String, Object> sessionCache;

    @Override
    public Boolean call() throws Exception {
        System.out.println("I am here: " + sessionCache);
        AdvancedCache<String, Object> cache = sessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
        for (String k: cache.keySet()) {
            if (isAReplicaKey(k)) {
                HAKieSerializedSession session = (HAKieSerializedSession) sessionCache.get(k);
                session.createSnapshot();
            }
        }
        return true;
    }

    private boolean isAReplicaKey(String key) {
        return !sessionCache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(sessionCache.getCacheManager().getAddress());
    }
}
