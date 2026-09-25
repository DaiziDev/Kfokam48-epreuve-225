package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests d'intégration EF1 : le trajet HTTP complet (contrat, validation,
 * advice, sérialisation) sur H2 avec migrations Flyway réelles.
 * Horloge figée à 2026-09-25T10:00:00Z pour vérifier RG1 au centième près.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(SessionControllerIntegrationTest.HorlogeFiguee.class)
class SessionControllerIntegrationTest {

    private static final Instant MAINTENANT_FIGE = Instant.parse("2026-09-25T10:00:00Z");

    @TestConfiguration
    static class HorlogeFiguee {
        @Bean
        @org.springframework.context.annotation.Primary
        Clock horlogeFiguee() {
            return Clock.fixed(MAINTENANT_FIGE, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotions;

    private Long promotionId;

    @BeforeEach
    void creerPromotion() {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("KFOKAM48");
        promotionId = promotions.save(promotion).getId();
    }

    @Test
    void laCreationRenvoie201AvecToutesLesDonnees() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.ouvertureAt").isString())
                .andExpect(jsonPath("$.expirationAt").isString())
                .andExpect(jsonPath("$.statut").value("OUVERTE"))
                .andReturn();

        JsonNode corps = objectMapper.readTree(resultat.getResponse().getContentAsString());
        assertThat(corps.get("code").asText()).hasSize(6);
    }

    @Test
    void expirationEgalOuverturePlus15Minutes() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode corps = objectMapper.readTree(resultat.getResponse().getContentAsString());
        Instant ouverture = Instant.parse(corps.get("ouvertureAt").asText());
        Instant expiration = Instant.parse(corps.get("expirationAt").asText());

        assertThat(ouverture).isEqualTo(MAINTENANT_FIGE);
        assertThat(expiration).isEqualTo(ouverture.plus(Duration.ofMinutes(15))); // RG1
    }

    @Test
    void titreManquantRenvoie400AuFormatImpose() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void promotionIdManquantRenvoie400AuFormatImpose() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void corpsJsonIllisibleRenvoie400EtPas500() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{titre invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CORPS_INVALIDE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
