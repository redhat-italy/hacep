package it.redhat.hacep.rules.reward.catalog;

import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

public class TestUtils {

    public static KieSession buildKieSession(KieBase kieBase) {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        KieSession session = kieBase.newKieSession(sessionConf, null);
        session.registerChannel(SysoutChannel.CHANNEL_ID, new SysoutChannel());
        session.registerChannel(AuditChannel.CHANNEL_ID, new AuditChannel());
        return session;
    }


    public static KieBase setupKieBase(String... resources) throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieBaseConfiguration config = ks.newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        KieFileSystem kfs = ks.newKieFileSystem();
        KieRepository kr = ks.getRepository();

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb);

        KieContainer kc = ks.newKieContainer(kr.getDefaultReleaseId());

        return kc.newKieBase(config);
    }

    private static void hasErrors(KieBuilder kbuilder) throws Exception {
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + kbuilder.getResults().toString());
        }
    }
}
