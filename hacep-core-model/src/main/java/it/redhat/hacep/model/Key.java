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

package it.redhat.hacep.model;

import org.infinispan.distribution.group.Group;

import java.io.Serializable;
import java.util.Objects;

public abstract class Key<T> implements Serializable {

    private String criteria;
    private String group;

    public Key(String criteria, String group) {
        this.criteria = criteria;
        this.group = group;
    }

    public abstract T getId();

    public String getCriteria() { return criteria; }

    @Group
    public final String getGroup() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key key = (Key) o;
        return Objects.equals(criteria, key.criteria) &&
                Objects.equals(group, key.group);
    }

    @Override
    public String toString() {
        return "Key for " + this.criteria + " -> " + this.group;
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, criteria);
    }
}
