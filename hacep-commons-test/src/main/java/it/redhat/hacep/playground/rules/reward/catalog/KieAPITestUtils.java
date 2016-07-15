package it.redhat.hacep.playground.rules.reward.catalog;

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.ClockType;
import org.drools.core.io.impl.ClassPathResource;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.KnowledgeBaseFactory;

public class KieAPITestUtils {

    public static KieSession buildKieSession(KieBase kieBase) {
        KieSessionConfiguration sessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        sessionConf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
        KieSession session = kieBase.newKieSession(sessionConf, null);
        return session;
    }


    public static KieBase setupKieBase(String... resources) throws Exception {
        ReleaseIdImpl releaseId = new ReleaseIdImpl("it.redhat.jdg", "rules", "1.0.0");
        KieContainer kieContainer = setupKieContainer(releaseId, "pom/pom-1.0.0.xml", resources);

        KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);

        return kieContainer.getKieBase();
    }

    public static KieContainer setupKieContainer(ReleaseId releaseId, String pom, String... resources) {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kfs = kieServices.newKieFileSystem();

        Resource pomResource = new ClassPathResource(pom);
        kfs.write("pom.xml", pomResource);

        Resource kmodule = new ClassPathResource("kmodule/kmodule.xml");
        kfs.write("src/main/resources/META-INF/kmodule.xml", kmodule);

        for (String res : resources) {
            Resource resource = new ClassPathResource(res);
            kfs.write("src/main/resources/" + res, resource);
        }

        KieBuilder kb = kieServices.newKieBuilder(kfs);
        kb.buildAll();
        hasErrors(kb);

        KieContainer kc = kieServices.newKieContainer(releaseId);

        return kc;
    }

    private static void hasErrors(KieBuilder kbuilder) {
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build errors\n" + kbuilder.getResults().toString());
        }
    }
}
