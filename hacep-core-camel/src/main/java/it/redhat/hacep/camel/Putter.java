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

import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Putter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Putter.class);

    private final Cache<Key, Fact> cache;

    public Putter(Cache<Key, Fact> cache) {
        this.cache = cache;
    }

    public void put(Fact fact) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Putting event in the grid");
        }
        if (cache != null) {
            fact.extractKeys().forEach(key -> cache.put(key, fact));
        }
    }
}
