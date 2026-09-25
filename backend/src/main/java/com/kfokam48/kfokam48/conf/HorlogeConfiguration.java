package com.kfokam48.kfokam48.conf;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Horloge injectable : le calcul de RG1 (expiration = ouverture + 15 min) passe
 * par ce bean, ce qui permet aux tests de figer le temps sans attendre ni mocker.
 */
@Configuration
public class HorlogeConfiguration {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
