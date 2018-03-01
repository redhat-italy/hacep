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

package it.redhat.hacep;

import it.redhat.hacep.cache.PutterImpl;
import it.redhat.hacep.cache.RulesUpdateVersionImpl;
import it.redhat.hacep.cache.listeners.*;
import it.redhat.hacep.cache.session.HAKieSessionBuilder;
import it.redhat.hacep.cache.session.KieSessionSaver;
import it.redhat.hacep.configuration.*;
import it.redhat.hacep.model.Fact;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class HACEPImpl implements HACEP {

    private final Logger LOGGER = LoggerFactory.getLogger(HACEPImpl.class);


    private final String nodeName;
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
    private RulesUpdateVersionImpl rulesUpdateVersion;
    private PutterImpl putter;

    public HACEPImpl() {
        this("hacep-node");
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public HACEPImpl(String nodeName) {
        this.nodeName = nodeName;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            try {
                this.rulesManager = new RulesManager(rulesConfiguration);
                this.dataGridManager = new DataGridManager();
                this.haKieSessionBuilder = new HAKieSessionBuilder(rulesManager, executorService);

                this.dataGridManager.startCacheInfo(nodeName);
                Cache<String, String> infoCache = this.dataGridManager.getReplicatedCache();
                String groupId = infoCache.putIfAbsent(RulesManager.RULES_GROUP_ID, rulesConfiguration.getGroupId());
                String artifactId = infoCache.putIfAbsent(RulesManager.RULES_ARTIFACT_ID, rulesConfiguration.getArtifactId());
                String version = infoCache.putIfAbsent(RulesManager.RULES_VERSION, rulesConfiguration.getVersion());
                this.rulesManager.start(groupId, artifactId, version);
                infoCache.addListener(new UpdateVersionListener(this.router, this.rulesManager));

                infoCache.putIfAbsent(Router.SUSPEND, "0");
                infoCache.putIfAbsent(Router.RESUME, "0");
                infoCache.addListener(new SuspendResumeListener(this.router));

                rulesUpdateVersion = new RulesUpdateVersionImpl(dataGridManager.getReplicatedCache());


                this.dataGridManager.start(haKieSessionBuilder, nodeName);

                this.dataGridManager.waitForMinimumOwners(1, TimeUnit.MINUTES);

                this.kieSessionSaver = new KieSessionSaver(haKieSessionBuilder, this.dataGridManager.getSessionCache());

                this.dataGridManager.getFactCache().addListener(new FactListenerPost(this.kieSessionSaver));
                this.dataGridManager.getSessionCache().addListener(new SessionListenerPre(this.router));
                this.dataGridManager.getSessionCache().addListener(new SessionListenerPost(this.router));

                putter = new PutterImpl(dataGridManager.getFactCache());
                this.router.start(jmsConfiguration, this);

                if (LOGGER.isDebugEnabled()) LOGGER.debug("Node [{}] finished starting.", this.nodeName);
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
                this.rulesManager.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void suspend() {
        //this.router.suspend();
        Cache<String, String> replicatedCache = dataGridManager.getReplicatedCache();
        replicatedCache.put(Router.SUSPEND, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void resume() {
        //this.router.resume();
        Cache<String, String> replicatedCache = dataGridManager.getReplicatedCache();
        replicatedCache.put(Router.RESUME, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public String info() {
        return dataGridManager.info();
    }

    @Override
    public String status() {
        return dataGridManager.status();
    }

    @Override
    public void insertFact(Fact fact) {
        putter.put(fact);
    }

    @Override
    public String update(String releaseId) {
        return rulesUpdateVersion.execute(releaseId);
    }

    @Override
    public EmbeddedCacheManager getCacheManager() {
        return dataGridManager.getCacheManager();
    }

    @Override
    public Cache<String, Object> getSessionCache() {
        return dataGridManager.getSessionCache();
    }

    public DataGridManager getDataGridManager() {
        return dataGridManager;
    }

    public RulesManager getRulesManager() {
        return rulesManager;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public JmsConfiguration getJmsConfiguration() {
        return jmsConfiguration;
    }

    public void setJmsConfiguration(JmsConfiguration jmsConfiguration) {
        this.jmsConfiguration = jmsConfiguration;
    }

    public RulesConfiguration getRulesConfiguration() {
        return rulesConfiguration;
    }

    public void setRulesConfiguration(RulesConfiguration rulesConfiguration) {
        this.rulesConfiguration = rulesConfiguration;
    }
}
