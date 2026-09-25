package com.kfokam48.kfokam48.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active les tâches planifiées (auto-clôture RG14). Coupée en profil test :
 * les tests appellent la règle directement avec une horloge déplaçable,
 * sans qu'un thread de fond ne modifie les données en parallèle.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "kfokam48.auto-cloture.active", havingValue = "true", matchIfMissing = true)
public class PlanificationConfiguration {
}
