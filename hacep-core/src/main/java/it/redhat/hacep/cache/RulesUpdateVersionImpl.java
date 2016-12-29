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

package it.redhat.hacep.cache;

import it.redhat.hacep.configuration.RulesManager;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesUpdateVersionImpl implements RulesUpdateVersion {

    private final static Logger LOGGER = LoggerFactory.getLogger(RulesUpdateVersionImpl.class);

    private final Cache<String, String> replicatedCache;

    public RulesUpdateVersionImpl(Cache<String, String> replicatedCache) {
        this.replicatedCache = replicatedCache;
    }

    @Override
    public String execute(String releaseId) {
        String[] tokens = releaseId.split(":");
        if (tokens.length == 3) {
            String groupId = tokens[0];
            String artifactId = tokens[1];
            String version = tokens[2];

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Ask for rules version update to %s:%s:%s", groupId, artifactId, version));
            }

            String expectedGroupId = replicatedCache.get(RulesManager.RULES_GROUP_ID);
            String expectedArtifactId = replicatedCache.get(RulesManager.RULES_ARTIFACT_ID);
            if (expectedGroupId.equals(groupId) && expectedArtifactId.equals(artifactId)) {
                String oldVersion = replicatedCache.get(RulesManager.RULES_VERSION);
                try {
                    replicatedCache.put(RulesManager.RULES_VERSION, version);
                } catch (Exception e) {
                    replicatedCache.put(RulesManager.RULES_VERSION, oldVersion);
                    throw new RuntimeException(e);
                }
                return releaseId;
            } else {
                throw new IllegalStateException("Update version in HACEP cannot change groupdId nor artifactId");
            }
        } else {
            throw new IllegalArgumentException("Illegal release id value [" + releaseId + "]");
        }
    }
}
