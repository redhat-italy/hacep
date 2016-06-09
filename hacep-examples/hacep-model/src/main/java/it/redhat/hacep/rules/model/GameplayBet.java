package it.redhat.hacep.rules.model;

import java.util.Date;
import java.util.Objects;

public class GameplayBet extends Gameplay {

    private static final long serialVersionUID = 1779732802814978929L;

    private long amount;

    public GameplayBet(long id, Long playerId, Date timestamp, long amount) {
        super(id, playerId, timestamp);
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameplayBet)) return false;
        if (!super.equals(o)) return false;
        GameplayBet that = (GameplayBet) o;
        return amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount);
    }

    @Override
    public String toString() {
        return "GamePlayBet{" +
                "amount=" + amount +
                "} " + super.toString();
    }
}
