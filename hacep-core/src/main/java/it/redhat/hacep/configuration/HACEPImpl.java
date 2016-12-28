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

package it.redhat.hacep.configuration;

import it.redhat.hacep.cache.PutterImpl;
import it.redhat.hacep.cache.listeners.FactListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPre;
import it.redhat.hacep.cache.listeners.UpdateVersionListener;
import it.redhat.hacep.cache.session.HAKieSessionBuilder;
import it.redhat.hacep.cache.session.KieSessionSaver;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class HACEPImpl implements HACEP {

    private DataGridManager dataGridManager;
    private RulesManager rulesManager;
    private ExecutorService executorService;
    private HAKieSessionBuilder haKieSessionBuilder;
    private KieSessionSaver kieSessionSaver;

    @Inject
    private Router router;

    @Inject
    private JmsConfiguration jmsConfiguration;

    @Inject
    private RulesConfiguration rulesConfiguration;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public HACEPImpl() {
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            try {
                this.rulesManager = new RulesManager(rulesConfiguration);
                this.dataGridManager = new DataGridManager();
                this.haKieSessionBuilder = new HAKieSessionBuilder(rulesManager, executorService);

                this.dataGridManager.start(haKieSessionBuilder);

                this.dataGridManager.waitForMinimumOwners(1, TimeUnit.MINUTES);

                this.kieSessionSaver = new KieSessionSaver(haKieSessionBuilder, this.dataGridManager.getSessionCache());

                this.dataGridManager.getFactCache().addListener(new FactListenerPost(this.kieSessionSaver));
                this.dataGridManager.getSessionCache().addListener(new SessionListenerPre(this.router));
                this.dataGridManager.getSessionCache().addListener(new SessionListenerPost(this.router));

                Cache<String, String> infoCache = this.dataGridManager.getReplicatedCache();
                String groupId = infoCache.putIfAbsent(RulesManager.RULES_GROUP_ID, rulesConfiguration.getGroupId());
                String artifactId = infoCache.putIfAbsent(RulesManager.RULES_ARTIFACT_ID, rulesConfiguration.getArtifactId());
                String version = infoCache.putIfAbsent(RulesManager.RULES_VERSION, rulesConfiguration.getVersion());
                this.rulesManager.start(groupId, artifactId, version);
                infoCache.addListener(new UpdateVersionListener(this.router, this.rulesManager));

                this.router.start(jmsConfiguration, new PutterImpl(dataGridManager.getFactCache()));
            } catch (Exception e) {
                started.set(false);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stop() {
        if (started.compareAndSet(true, false)) {
            try {
                this.router.stop();
                this.dataGridManager.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void suspend() {
        this.router.suspend();
    }

    @Override
    public void resume() {
        this.router.resume();
    }

    @Override
    public void removeKey(Key key) {
        dataGridManager.removeSession(key);
    }

    @Override
    public EmbeddedCacheManager getCacheManager() {
        return dataGridManager.getCacheManager();
    }

    @Override
    public Cache<String, Object> getSessionCache() {
        return dataGridManager.getSessionCache();
    }

}
