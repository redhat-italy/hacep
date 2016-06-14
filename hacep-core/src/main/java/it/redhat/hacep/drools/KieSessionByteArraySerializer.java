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

import it.redhat.hacep.compressor.Compressor;
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

    private static final Logger logger = LoggerFactory.getLogger("it.redhat.hacep");
    private final Compressor compressor = new Compressor();
    private final boolean compressed;
    private final DroolsConfiguration droolsConfiguration;

    public KieSessionByteArraySerializer(DroolsConfiguration droolsConfiguration, boolean compressed) {
        this.droolsConfiguration = droolsConfiguration;
        this.compressed = compressed;
    }

    public byte[] writeObject(KieSession kieSession) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream);) {
            /*
             * It seems that the Marshaller does not persist the actual SessionClock, which is a problem when using the PseudoClock, so we
             * persist the SessionConfiguration, Environment and clock time to be able to execute the pseudo-clock (if it's used).
             */
            KieSessionConfiguration kieSessionConfiguration = kieSession.getSessionConfiguration();
            oos.writeObject(kieSessionConfiguration);

            Marshaller marshaller = createSerializableMarshaller(kieSession.getKieBase());
            marshaller.marshall(outputStream, kieSession);

            byte[] bytes = outputStream.toByteArray();
            int uncompressedSize = bytes.length;
            if (compressed) {
                bytes = compressor.compress(bytes);
                logger.info("Size of session is: " + bytes.length + "  [" + uncompressedSize + "]");
            }
            logger.info("Size of session is: " + bytes.length);
            return bytes;
        } catch (IOException ioe) {
            String errorMessage = "Unable to marshall KieSession.";
            logger.error(errorMessage, ioe);
            throw new RuntimeException(errorMessage, ioe);
        }
    }

    public KieSession readSession(byte[] serializedKieSession) {

        if (serializedKieSession == null) {
            logger.warn("[KieSessionByteArraySerializer] Unable to serialize NULL KieSessions");
            return null;
        }
        ObjectInputStream ois = null;
        ByteArrayInputStream inputStream = null;
        try {
            if (compressed) {
                inputStream = new ByteArrayInputStream(compressor.decompress(serializedKieSession));
            } else {
                inputStream = new ByteArrayInputStream(serializedKieSession);
            }
            ois = new ObjectInputStream(inputStream);
            KieSessionConfiguration kieSessionConfiguration = (KieSessionConfiguration) ois.readObject();
            Marshaller marshaller = createSerializableMarshaller(droolsConfiguration.getKieBase());
            KieSession kieSession = marshaller.unmarshall(inputStream, kieSessionConfiguration, null);
            return kieSession;
        } catch (Exception e) {
            logger.error("Error when reading serialized session", e);
            throw new RuntimeException(e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }

        }
    }

    private Marshaller createSerializableMarshaller(KieBase kBase) {
        ObjectMarshallingStrategyAcceptor acceptor = MarshallerFactory.newClassFilterAcceptor(new String[]{"*.*"});
        ObjectMarshallingStrategy strategy = MarshallerFactory.newSerializeMarshallingStrategy(acceptor);
        Marshaller marshaller = MarshallerFactory.newMarshaller(kBase, new ObjectMarshallingStrategy[]{strategy});
        return marshaller;
    }

}
