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
import org.infinispan.remoting.transport.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JDG {

    public static String routingTable(Cache<Key, Object> cache) {
        return cache.getAdvancedCache().getDistributionManager().getConsistentHash().getRoutingTableAsString();
    }

    public static List<Address> locate(Cache<Key, Object> cache, long id) {
        return cache.getAdvancedCache().getDistributionManager().getConsistentHash().locateOwners(id);
    }

    public static Address locatePrimary(Cache<Key, Object> cache, Key id) {
        return cache.getAdvancedCache().getDistributionManager().getConsistentHash().locatePrimaryOwner(id);
    }

    public static boolean checkIfCacheIsPrimaryFor(Cache<Key, Object> cache, Key key) {
        return cache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(cache.getCacheManager().getAddress());
    }

    public static boolean checkIfKeyIsLocalInCache(Cache<Key, Object> cache, Key key) {
        return cache.getAdvancedCache().getDistributionManager().getLocality(key).isLocal();
    }

    public static boolean checkIfCacheIsSecondaryFor(Cache<Key, Object> cache, Key key) {
        return !checkIfCacheIsPrimaryFor(cache, key) && checkIfKeyIsLocalInCache(cache, key);
    }

    public static Set<String> valuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, Filter.ALL);
    }

    public static Set<String> localValuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, Filter.LOCAL);
    }

    public static Set<String> primaryValuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, Filter.PRIMARY);
    }

    public static Set<String> replicaValuesFromKeys(Cache<Key, Object> cache) {
        return valuesFromKeys(cache, Filter.REPLICA);
    }

    private static Set<String> valuesFromKeys(Cache<Key, Object> cache, Filter filter) {
        Set<String> values = new HashSet<String>();

        for (Key l : cache.keySet()) {
            switch (filter) {
                case ALL:
                    values.add(l + " " + cache.get(l));
                    break;
                case LOCAL:
                    if (checkIfKeyIsLocalInCache(cache, l)) {
                        values.add(l + " " + cache.get(l));
                    }
                    break;
                case PRIMARY:
                    if (checkIfCacheIsPrimaryFor(cache, l)) {
                        values.add(l + " " + cache.get(l));
                    }
                    break;
                case REPLICA:
                    if (checkIfCacheIsSecondaryFor(cache, l)) {
                        values.add(l + " " + cache.get(l));
                    }
                    break;
            }
        }
        return values;
    }

    private static final Logger log = LoggerFactory.getLogger(JDG.class);

    private static enum Filter {ALL, LOCAL, PRIMARY, REPLICA}

    ;

}
