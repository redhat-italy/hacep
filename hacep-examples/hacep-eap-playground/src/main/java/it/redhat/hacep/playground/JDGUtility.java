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

package it.redhat.hacep.playground;

import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class JDGUtility {

    private final static Logger log = LoggerFactory.getLogger(JDGUtility.class);

    public Set<String> valuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, e -> true);
    }

    public Set<String> localValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        return valuesFromKeys(cache, k -> distributionManager.getLocality(k).isLocal());
    }

    public Set<String> primaryValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        Address address = cache.getCacheManager().getAddress();
        return valuesFromKeys(cache, k -> distributionManager.getPrimaryLocation(k).equals(address));
    }

    public Set<String> replicaValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        Address address = cache.getCacheManager().getAddress();
        return valuesFromKeys(cache, k -> distributionManager.getLocality(k).isLocal() && !distributionManager.getPrimaryLocation(k).equals(address));
    }

    public Map<Key, List<Address>> getKeysAddresses(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        Map<Key, List<Address>> response = new HashMap<>();
        for(Key k : cache.keySet()) {
            response.put(k, distributionManager.locate(k));
        }
        return response;
    }

    private Set<String> valuesFromKeys(Cache<Key, Object> cache,
                                       Predicate<Key> predicate) {
        Set<String> response = new HashSet<>();
        for(Key k : cache.keySet()) {
            if(predicate.test(k)) {
                response.add(k + " " + cache.get(k));
            }
        }
        return response;
    }

}
