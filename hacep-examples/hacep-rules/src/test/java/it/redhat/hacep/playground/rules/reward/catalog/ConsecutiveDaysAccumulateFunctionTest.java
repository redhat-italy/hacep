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

package it.redhat.hacep.playground.rules.reward.catalog;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import it.redhat.hacep.playground.rules.reward.catalog.ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets;

public class ConsecutiveDaysAccumulateFunctionTest {
    
    @Test
    public void testCreateContext() {
        ConsecutiveDaysAccumulateFunction f = new ConsecutiveDaysAccumulateFunction();
        Object o = f.createContext();
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets);
        ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) o;
        Assert.assertNull(buckets.buckets);
    }
    
    @Test
    public void testInit() throws Exception {
        ConsecutiveDaysAccumulateFunction f = new ConsecutiveDaysAccumulateFunction();
        ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets buckets = new ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets();
        f.init(buckets);
        Assert.assertNotNull(buckets.buckets);
        Assert.assertEquals(0, buckets.buckets.size());
    }
    
    @Test
    public void testAccumulateAndResult() throws Exception {
        
        long delta = TimeUnit.HOURS.toMillis(10);
        
        ConsecutiveDaysAccumulateFunction f = new ConsecutiveDaysAccumulateFunction();
        ConsecutiveDaysAccumulateFunction.ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) f.createContext();
        f.init(buckets);
        f.accumulate(buckets, delta);
        Assert.assertEquals(1, buckets.buckets.size());
        Assert.assertNotNull(buckets.buckets.get(0));
        Assert.assertEquals(1, buckets.buckets.get(0).intValue());
        Assert.assertTrue(f.getResult(buckets) instanceof Number);
        Assert.assertEquals(1, ((Number)f.getResult(buckets)).intValue());
        
        delta = TimeUnit.HOURS.toMillis(12);
        f.accumulate(buckets, delta);
        Assert.assertEquals(2, buckets.buckets.get(0).intValue());
        Assert.assertEquals(1, ((Number)f.getResult(buckets)).intValue());
        
        delta = TimeUnit.DAYS.toMillis(1);
        f.accumulate(buckets, delta);
        Assert.assertEquals(2, buckets.buckets.size());
        Assert.assertNotNull(buckets.buckets.get(1));
        Assert.assertEquals(1, buckets.buckets.get(1).intValue());
        Assert.assertEquals(2, ((Number)f.getResult(buckets)).intValue());
        
        delta = TimeUnit.DAYS.toMillis(1) + TimeUnit.SECONDS.toMillis(1);
        f.accumulate(buckets, delta);
        Assert.assertEquals(2, buckets.buckets.size());
        Assert.assertNotNull(buckets.buckets.get(1));
        Assert.assertEquals(2, buckets.buckets.get(1).intValue());
        Assert.assertEquals(2, ((Number)f.getResult(buckets)).intValue());        
    }

}
