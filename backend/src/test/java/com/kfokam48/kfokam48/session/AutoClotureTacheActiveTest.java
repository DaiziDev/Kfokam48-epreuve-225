package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * EF14 : vérifie le câblage de la tâche planifiée telle qu'elle tourne hors
 * test (bean présent, intervalle PT1M accepté par @Scheduled au démarrage).
 */
@SpringBootTest(properties = "kfokam48.auto-cloture.active=true")
@ActiveProfiles("test")
class AutoClotureTacheActiveTest {

    @Autowired
    private ApplicationContext contexte;

    @Test
    void laTacheEstEnregistreeQuandLAutoClotureEstActive() {
        assertThat(contexte.getBeansOfType(AutoClotureTache.class)).hasSize(1);
    }
}
