package it.redhat.hacep.model;

import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;

public interface KeyBuilder<F extends Fact, T> {

    Key<T> extractFromFact(F fact);
}
