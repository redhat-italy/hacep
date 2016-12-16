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
import it.redhat.hacep.cache.session.HAKieSession;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cdi.embedded.Input;
import org.infinispan.context.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class Snapshotter implements Callable<Boolean>, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Snapshotter.class);

    private static final long serialVersionUID = 5771231232134508L;

    @Inject
    @Input
    private Cache<String, HAKieSession> sessionCache;

    public Snapshotter() {
    }

    @Override
    public Boolean call() throws Exception {
        System.out.println(String.format("Called snapshot on cache [%s]", sessionCache));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Called snapshot on cache [%s]", sessionCache));
        }
        AdvancedCache<String, HAKieSession> cache = sessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
        for (String k : cache.keySet()) {
            HAKieSession session = sessionCache.get(k);
            if (session != null && session.isSerialized()) {
                ((HAKieSerializedSession) session).createSnapshot();
                ((HAKieSerializedSession) session).waitForSnapshotToComplete();
            }
        }
        return true;
    }

}
