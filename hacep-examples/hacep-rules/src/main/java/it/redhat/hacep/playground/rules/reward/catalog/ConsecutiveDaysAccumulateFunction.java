package it.redhat.hacep.playground.rules.reward.catalog;

import org.kie.api.runtime.rule.AccumulateFunction;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsecutiveDaysAccumulateFunction implements AccumulateFunction {

    @Override
    public void accumulate(Serializable context, Object value) {
        ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) context;
        int days = getDays(value);
        if (buckets.buckets.get(days) == null) {
            buckets.buckets.put(days, new Integer(1));
        } else {
            buckets.buckets.put(days, buckets.buckets.get(days) + 1);
        }
    }

    @Override
    public Serializable createContext() {
        return new ConsecutiveDaysBuckets();
    }

    @Override
    public Object getResult(Serializable context) throws Exception {
        ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) context;
        return buckets.buckets.size();
    }

    @Override
    public void init(Serializable context) throws Exception {
        ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) context;
        buckets.buckets = new HashMap<>();
    }

    @Override
    public void reverse(Serializable context, Object value) throws Exception {
        ConsecutiveDaysBuckets buckets = (ConsecutiveDaysBuckets) context;
        int days = getDays(value);
        if (buckets.buckets.get(days) == null) {
            //ignore, shouldn't happen
        } else if (buckets.buckets.get(days) == 1) {
            buckets.buckets.remove(days);
        } else {
            buckets.buckets.put(days, buckets.buckets.get(days) - 1);
        }
    }

    @Override
    public boolean supportsReverse() {
        return true;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<?> getResultType() {
        return Number.class;
    }

    public static class ConsecutiveDaysBuckets implements Externalizable {

        public Map<Integer, Integer> buckets;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(buckets.size());
            for (int key : buckets.keySet()) {
                out.writeInt(key);
                out.writeInt(buckets.get(key));
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            buckets = new HashMap<>();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                buckets.put(in.readInt(), in.readInt());
            }
        }
    }

    private int getDays(Object value) {
        BigDecimal data = new BigDecimal(((Number) value).longValue());
        int days = data.divide(new BigDecimal(TimeUnit.DAYS.toMillis(1)), 2, RoundingMode.HALF_UP).intValue();
        return days;
    }

}
