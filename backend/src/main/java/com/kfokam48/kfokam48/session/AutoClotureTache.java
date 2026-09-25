package com.kfokam48.kfokam48.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * EF14/RG14 : déclenche périodiquement la clôture automatique. La règle vit
 * dans SessionService (testable à horloge déplaçable) ; cette classe ne fait
 * que la planifier. Désactivable par propriété — c'est le cas en profil test.
 */
@Component
@ConditionalOnProperty(name = "kfokam48.auto-cloture.active", havingValue = "true", matchIfMissing = true)
public class AutoClotureTache {

    private static final Logger LOG = LoggerFactory.getLogger(AutoClotureTache.class);

    private final SessionService service;

    public AutoClotureTache(SessionService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${kfokam48.auto-cloture.intervalle:PT1M}")
    public void executer() {
        int cloturees = service.cloturerSessionsEchues();
        if (cloturees > 0) {
            LOG.info("Auto-clôture (RG14) : {} session(s) clôturée(s)", cloturees);
        }
    }
}
