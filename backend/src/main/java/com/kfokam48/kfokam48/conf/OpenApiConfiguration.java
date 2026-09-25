package com.kfokam48.kfokam48.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Métadonnées Swagger reprises du contrat d'API (api/contrat.yml).
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI contratOpenApi() {
        return new OpenAPI().info(new Info()
                .title("KFOKAM48 — Présence & Relecture")
                .version("1.0")
                .description("Contrat imposé complété : sessions, présences, exercices, relectures, tableau de suivi. "
                        + "Format d'erreur imposé { code, message } sur toutes les erreurs."));
    }
}
