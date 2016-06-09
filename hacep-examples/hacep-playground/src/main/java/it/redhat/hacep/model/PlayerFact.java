package it.redhat.hacep.model;

import java.time.Instant;
import java.util.Date;

public class PlayerFact implements Fact {

    private static final long serialVersionUID = 7517352753296362943L;

    private long id;

    private Date timestamp;

    private Long playerId;

    @Override
    public Instant getDateTime() {
        return timestamp.toInstant();
    }
}
