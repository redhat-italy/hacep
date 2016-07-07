/**
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

package it.redhat.hacep;

import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class JDGUtility {

    public Set<String> valuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, e -> true);
    }

    public Set<String> localValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        return valuesFromKeys(cache, e -> distributionManager.getLocality(e.getKey()).isLocal());
    }

    public Set<String> primaryValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        Address address = cache.getCacheManager().getAddress();
        return valuesFromKeys(cache, e -> distributionManager.getPrimaryLocation(e.getKey()).equals(address));
    }

    public Set<String> replicaValuesFromKeys(Cache<Key, Object> cache) {
        DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        Address address = cache.getCacheManager().getAddress();
        return valuesFromKeys(cache, e -> distributionManager.getLocality(e.getKey()).isLocal() && distributionManager.getPrimaryLocation(e.getKey()).equals(address));
    }

    private Set<String> valuesFromKeys(Cache<Key, Object> cache,
                                       Predicate<Map.Entry<Key, Object>> predicate) {
        return cache.entrySet().stream()
                .filter(predicate)
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.toSet());
    }
}
