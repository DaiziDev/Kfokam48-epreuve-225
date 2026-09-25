package com.kfokam48.kfokam48.session;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Horloge de test déplaçable : le bean est créé UNE fois et injecté dans les
 * services, c'est donc l'instance elle-même qui doit pouvoir bouger (remplacer
 * le champ d'une @TestConfiguration ne suffirait pas — les services garderaient
 * l'ancien objet).
 */
class HorlogeMutable extends Clock {

    private volatile Instant instant;

    HorlogeMutable(Instant initial) {
        this.instant = initial;
    }

    void avancerA(Instant nouvelInstant) {
        this.instant = nouvelInstant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
