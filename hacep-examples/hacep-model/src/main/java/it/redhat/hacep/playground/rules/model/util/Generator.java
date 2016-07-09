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
