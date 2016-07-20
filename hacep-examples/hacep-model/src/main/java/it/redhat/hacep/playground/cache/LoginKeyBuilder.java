package it.redhat.hacep.playground.cache;

import it.redhat.hacep.model.Key;
import it.redhat.hacep.model.KeyBuilder;
import it.redhat.hacep.playground.rules.model.UserEvent;

public class LoginKeyBuilder implements KeyBuilder<UserEvent, String> {

    @Override
    public Key<String> extractFromFact(UserEvent fact) {
        return new LoginKey(String.valueOf(fact.getId()), String.valueOf(fact.getUsr()));
    }
}
