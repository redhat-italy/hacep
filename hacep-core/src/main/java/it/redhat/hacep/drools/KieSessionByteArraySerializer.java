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

package it.redhat.hacep.drools;

import it.redhat.hacep.configuration.DroolsConfiguration;
import org.kie.api.KieBase;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.internal.marshalling.MarshallerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class KieSessionByteArraySerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("it.redhat.hacep");

    private final DroolsConfiguration droolsConfiguration;

    public KieSessionByteArraySerializer(DroolsConfiguration droolsConfiguration) {
        this.droolsConfiguration = droolsConfiguration;
    }

    public byte[] writeObject(KieSession kieSession) {
        Marshaller marshaller = createSerializableMarshaller(droolsConfiguration.getKieBase());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            /*
             * It seems that the Marshaller does not persist the actual SessionClock, which is a problem when using the PseudoClock, so we
             * persist the SessionConfiguration, Environment and clock time to be able to execute the pseudo-clock (if it's used).
             */
            KieSessionConfiguration kieSessionConfiguration = kieSession.getSessionConfiguration();
            oos.writeObject(kieSessionConfiguration);

            marshaller.marshall(outputStream, kieSession);
            outputStream.flush();
            byte[] bytes = outputStream.toByteArray();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Size of session is: " + bytes.length);
            }
            return bytes;
        } catch (IOException ioe) {
            String errorMessage = "Unable to marshall KieSession.";
            LOGGER.error(errorMessage, ioe);
            throw new RuntimeException(errorMessage, ioe);
        }
    }

    public KieSession readSession(byte[] buffer) {
        if (buffer == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("[KieSessionByteArraySerializer] Unable to serialize NULL KieSessions");
            }
            return null;
        }
        Marshaller marshaller = createSerializableMarshaller(droolsConfiguration.getKieBase());
        System.out.println("Read from Serialized buffer [" + buffer.length + "]");
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
             ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            KieSessionConfiguration kieSessionConfiguration = (KieSessionConfiguration) ois.readObject();
            return marshaller.unmarshall(inputStream, kieSessionConfiguration, null);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("Error when reading serialized session", e);
            throw new RuntimeException(e);
        }
    }

    private Marshaller createSerializableMarshaller(KieBase kBase) {
        ObjectMarshallingStrategyAcceptor acceptor = MarshallerFactory.newClassFilterAcceptor(new String[]{"*.*"});
        ObjectMarshallingStrategy strategy = MarshallerFactory.newSerializeMarshallingStrategy(acceptor);
        return MarshallerFactory.newMarshaller(kBase, new ObjectMarshallingStrategy[]{strategy});
    }

}
