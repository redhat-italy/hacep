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

package it.redhat.hacep.compressor;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DataFormatException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class CompressorTest {


    @Test
    public void testZeroFilledBufferCompression() throws IOException, DataFormatException {
        Compressor compressor = new Compressor();
        byte[] session = new byte[30000];
        Arrays.fill(session, (byte) 0);
        long before = System.currentTimeMillis();
        byte[] compressed = compressor.compress(session);
        byte[] session1 = compressor.decompress(compressed);
        long after = System.currentTimeMillis();

        System.out.println("Compression time: " + (after - before));
        assertTrue(compressed.length < session.length);
        assertArrayEquals(session, session1);
    }

    @Test
    public void testCompression() throws IOException, DataFormatException {
        Compressor compressor = new Compressor();
        byte[] session = new byte[30000];
        new Random().nextBytes(session);
        long before = System.currentTimeMillis();
        byte[] compressed = compressor.compress(session);
        byte[] session1 = compressor.decompress(compressed);
        long after = System.currentTimeMillis();

        System.out.println("Compression time: " + (after - before));
        assertArrayEquals(session, session1);
    }

}
