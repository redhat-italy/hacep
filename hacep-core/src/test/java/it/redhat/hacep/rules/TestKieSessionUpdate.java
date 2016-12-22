package it.redhat.hacep.rules;

import it.redhat.hacep.cluster.TestFact;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.playground.rules.reward.catalog.KieAPITestUtils;
import it.redhat.hacep.support.KieSessionUtils;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.io.impl.ClassPathResource;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.marshalling.MarshallerFactory;
import org.kie.scanner.MavenRepository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

public class TestKieSessionUpdate {

    private ZonedDateTime now = ZonedDateTime.now();

    @Test
    public void testContainerUpdate() throws IOException, URISyntaxException {
        ArrayList<Object> globalList = new ArrayList<>();

        KieServices ks = KieServices.Factory.get();

        ReleaseIdImpl releaseIdV1 = new ReleaseIdImpl("it.redhat.test.update", "rules", "1.0.0");
        installRelease(releaseIdV1, "pom/pom-1.0.0.xml", "rules/globals-v1-rule.drl");

        KieContainer kieContainer = ks.newKieContainer(releaseIdV1);

        KieSession kieSession = kieContainer.newKieSession();
        kieSession.setGlobal("list", globalList);

        Assert.assertEquals(0, globalList.size());

        kieSession.insert(1L);
        kieSession.fireAllRules();

        Assert.assertEquals(1, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));

        kieSession.insert(2L);
        kieSession.fireAllRules();

        Assert.assertEquals(2, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));

        ReleaseIdImpl releaseIdV2 = new ReleaseIdImpl("it.redhat.test.update", "rules", "2.0.0");
        installRelease(releaseIdV2, "pom/pom-2.0.0.xml", "rules/globals-v2-rule.drl");

        kieContainer.updateToVersion(releaseIdV2);
        kieSession.insert(4L);
        kieSession.fireAllRules();

        Assert.assertEquals(3, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));
        Assert.assertEquals(14L, globalList.get(2));

        kieSession.dispose();
    }

    @Test
    public void testContainerUpdateAfterSerialization() throws IOException, URISyntaxException {
        ArrayList<Object> globalList = new ArrayList<>();

        KieServices ks = KieServices.Factory.get();

        ReleaseIdImpl releaseIdV1 = new ReleaseIdImpl("it.redhat.test.serialized", "rules", "1.0.0");
        installRelease(releaseIdV1, "pom/pom-1.0.0.xml", "rules/globals-v1-rule.drl");

        KieContainer kieContainer = ks.newKieContainer(releaseIdV1);

        KieSession kieSession = kieContainer.newKieSession();
        kieSession.setGlobal("list", globalList);

        Assert.assertEquals(0, globalList.size());

        kieSession.insert(1L);
        kieSession.fireAllRules();

        Assert.assertEquals(1, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));

        kieSession.insert(2L);
        kieSession.fireAllRules();

        Assert.assertEquals(2, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));

        KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer();
        byte[] buffer = serializer.writeObject(createSerializableMarshaller(kieContainer.getKieBase()), kieSession);
        kieSession.dispose();

        ReleaseIdImpl releaseIdV2 = new ReleaseIdImpl("it.redhat.test.serialized", "rules", "2.0.0");
        installRelease(releaseIdV2, "pom/pom-2.0.0.xml", "rules/globals-v2-rule.drl");

        kieContainer.updateToVersion(releaseIdV2);

        KieSession serializedSession = serializer.readSession(createSerializableMarshaller(kieContainer.getKieBase()), buffer);
        serializedSession.setGlobal("list", globalList);

        serializedSession.insert(4L);
        serializedSession.fireAllRules();

        Assert.assertEquals(3, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));
        Assert.assertEquals(14L, globalList.get(2));
    }

    @Test
    public void testContainerUpdateAfterSerializationWindowLength() throws IOException, URISyntaxException {
        ArrayList<Object> globalList = new ArrayList<>();

        KieServices ks = KieServices.Factory.get();

        ReleaseIdImpl releaseIdV1 = new ReleaseIdImpl("it.redhat.test.serialized.window", "rules", "1.0.0");
        installRelease(releaseIdV1, "pom/pom-1.0.0.xml", "rules/simple-rule.drl");

        KieContainer kieContainer = ks.newKieContainer(releaseIdV1);

        KieSession kieSession = kieContainer.newKieSession();
        kieSession.registerChannel("additions", globalList::add);

        Assert.assertEquals(0, globalList.size());

        Fact fact = generateFactTenSecondsAfter(1L, 1L);
        KieSessionUtils.advanceClock(kieSession, fact);
        kieSession.insert(fact);
        kieSession.fireAllRules();

        Assert.assertEquals(1, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));

        fact = generateFactTenSecondsAfter(1L, 2L);
        KieSessionUtils.advanceClock(kieSession, fact);
        kieSession.insert(fact);
        kieSession.fireAllRules();

        Assert.assertEquals(2, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));

        KieSessionByteArraySerializer serializer = new KieSessionByteArraySerializer();
        byte[] buffer = serializer.writeObject(createSerializableMarshaller(kieContainer.getKieBase()), kieSession);
        kieSession.dispose();

        ReleaseIdImpl releaseIdV2 = new ReleaseIdImpl("it.redhat.test.serialized.window", "rules", "2.0.0");
        installRelease(releaseIdV2, "pom/pom-2.0.0.xml", "rules/simple-rule_modified.drl");

        KieSession serializedSession = serializer.readSession(createSerializableMarshaller(kieContainer.getKieBase()), buffer);
        serializedSession.registerChannel("additions", globalList::add);

        //KieContainer update should be done after unmarshall
        kieContainer.updateToVersion(releaseIdV2);

        fact = generateFactTenSecondsAfter(1L, 4L);
        KieSessionUtils.advanceClock(serializedSession, fact);
        serializedSession.insert(fact);
        serializedSession.fireAllRules();

        Assert.assertEquals(3, globalList.size());
        Assert.assertEquals(1L, globalList.get(0));
        Assert.assertEquals(3L, globalList.get(1));
        Assert.assertEquals(14L, globalList.get(2));
    }

    private void installRelease(ReleaseIdImpl releaseId, String pom, String... rules) throws URISyntaxException, IOException {
        KieModule kieModule = KieAPITestUtils.createKieModule(releaseId, pom, rules);
        ClassPathResource resource = new ClassPathResource(pom);
        File file = new File(resource.getURL().toURI());
        MavenRepository.getMavenRepository().installArtifact(releaseId, (InternalKieModule) kieModule, file);
    }

    private Marshaller createSerializableMarshaller(KieBase kieBase) {
        ObjectMarshallingStrategyAcceptor acceptor = MarshallerFactory.newClassFilterAcceptor(new String[]{"*.*"});
        ObjectMarshallingStrategy strategy = MarshallerFactory.newSerializeMarshallingStrategy(acceptor);
        Marshaller marshaller = MarshallerFactory.newMarshaller(kieBase, new ObjectMarshallingStrategy[]{strategy});
        return marshaller;
    }

    private Fact generateFactTenSecondsAfter(long ppid, long amount) {
        now = now.plusSeconds(10);
        return new TestFact(ppid, amount, new Date(now.toInstant().toEpochMilli()));
    }
}
