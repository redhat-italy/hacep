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

package it.redhat.hacep.playground.rules.model.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public abstract class Generator<T> {

    public static final int MODE_RANDOM = 0;
    public static final int MODE_INTERVAL = 1;

    private final List<T> list = new CopyOnWriteArrayList<>();
    private long from;
    private long duration;

    private int count = 1;

    protected abstract T build(long ts);

    public final Generator count(int count) {
        this.count = count;
        return this;
    }

    public final Generator timestamp(long from, long duration, TimeUnit unit) {
        this.from = from;
        this.duration = unit.toMillis(duration);
        return this;
    }

    public final List<T> generate() {
        long ts = this.from;
        long delta = (duration / count);
        for (int i = 0; i < count; i++) {
            ts += delta;
            list.add(build(ts));
        }
        return list;
    }

    public final Generator reset() {
        list.clear();
        return this;
    }

    public final List<T> getGenerated() {
        return list;
    }
}
