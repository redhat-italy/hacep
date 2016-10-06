package it.redhat.hacep.cluster;

import java.io.Serializable;
import java.util.Date;

public class TestRuleLocker implements Serializable{

    private long ppid;
    private Date timestamp;
    private long ttl;

    public TestRuleLocker(long ppid, Date timestamp, long ttl) {
        this.ppid = ppid;
        this.timestamp = timestamp;
        this.ttl = ttl;
    }

    public long getPpid() {
        return ppid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public long getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return "TestRuleLocker{" +
                "ppid=" + ppid +
                ", timestamp=" + timestamp +
                ", ttl=" + ttl +
                '}';
    }
}
