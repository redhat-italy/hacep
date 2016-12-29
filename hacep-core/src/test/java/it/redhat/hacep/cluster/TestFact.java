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

package it.redhat.hacep.cluster;

import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class TestFact implements Fact {

    private final long ppid;
    private final long amount;
    private final Date instant;
    private final Key key;

    public TestFact(long ppid, long amount, Date instant, Key key) {
        this.ppid = ppid;
        this.amount = amount;
        this.instant = instant;
        this.key = key;
    }

    public long getPpid() {
        return ppid;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public Instant getInstant() {
        return instant.toInstant();
    }

    @Override
    public Key extractKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestFact)) return false;
        TestFact testFact = (TestFact) o;
        return ppid == testFact.ppid &&
                amount == testFact.amount &&
                Objects.equals(instant, testFact.instant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ppid, amount, instant);
    }

    @Override
    public String toString() {
        return "TestFact{" +
                "ppid=" + ppid +
                ", amount=" + amount +
                ", instant=" + instant +
                '}';
    }
}
