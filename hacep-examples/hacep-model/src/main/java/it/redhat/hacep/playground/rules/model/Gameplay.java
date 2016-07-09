package it.redhat.hacep.playground.rules.model;

import it.redhat.hacep.model.Fact;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class Gameplay implements Fact {

    private static final long serialVersionUID = 7517352753296362943L;

    protected long id;

    protected Long playerId;

    protected Date timestamp;

    public Gameplay(long id, Long playerId, Date timestamp) {
        this.id = id;
        this.playerId = playerId;
        this.timestamp = timestamp;
    }

    @Override
    public Instant getInstant() {
        return timestamp.toInstant();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Gameplay)) return false;
        Gameplay gameplay = (Gameplay) o;
        return id == gameplay.id &&
                Objects.equals(playerId, gameplay.playerId) &&
                Objects.equals(timestamp, gameplay.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerId, timestamp);
    }

    @Override
    public String toString() {
        return "GamePlay{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", timestamp=" + timestamp +
                '}';
    }
}
