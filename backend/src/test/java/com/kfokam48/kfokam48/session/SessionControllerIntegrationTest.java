package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests d'intégration EF1, EF13 et EF15 : le trajet HTTP complet (contrat, validation,
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

    // --- Liste des sessions (frontend : retrouver ses sessions) ---

    @Test
    void listeLesSessionsDeLaPromotionAvecTitreEtStatut() throws Exception {
        mockMvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Séance 1\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated());
        MvcResult seconde = mockMvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Séance 2\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSeconde = objectMapper.readTree(seconde.getResponse().getContentAsString()).get("id").asLong();

        // Horloge figée : même ouvertureAt, l'identifiant départage (plus récente d'abord)
        mockMvc.perform(get("/api/sessions").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(idSeconde))
                .andExpect(jsonPath("$[0].titre").value("Séance 2"))
                .andExpect(jsonPath("$[0].code").isString())
                .andExpect(jsonPath("$[0].statut").value("OUVERTE"))
                .andExpect(jsonPath("$[1].titre").value("Séance 1"));
    }

    @Test
    void listeDesSessionsPromotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/sessions").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    // --- EF13/EF15/RG15 : clôture et réouverture manuelles ---

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private EtudiantRepository etudiants;

    @Autowired
    private EntityManager entityManager;

    private Long creerSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions cloturer(Long sessionId) throws Exception {
        return mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture"));
    }

    private ResultActions rouvrir(Long sessionId) throws Exception {
        return mockMvc.perform(patch("/api/sessions/" + sessionId + "/reouverture"));
    }

    @Test
    void cloturerUneSessionOuverteLaPasseACloturee() throws Exception {
        Long sessionId = creerSession();

        cloturer(sessionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.statut").value("CLOTUREE"));

        assertThat(sessions.findById(sessionId).orElseThrow().getStatut()).isEqualTo(SessionStatut.CLOTUREE);
    }

    @Test
    void cloturerUneSessionDejaClotureeRenvoie409() throws Exception {
        Long sessionId = creerSession();
        cloturer(sessionId).andExpect(status().isOk());

        cloturer(sessionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_CLOTUREE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void cloturerUneSessionInconnueRenvoie404() throws Exception {
        cloturer(999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void rouvrirUneSessionClotureeLaRepasseAOuverte() throws Exception {
        Long sessionId = creerSession();
        cloturer(sessionId).andExpect(status().isOk());

        rouvrir(sessionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.statut").value("OUVERTE"));

        assertThat(sessions.findById(sessionId).orElseThrow().getStatut()).isEqualTo(SessionStatut.OUVERTE);
    }

    @Test
    void rouvrirUneSessionDejaOuverteRenvoie409() throws Exception {
        Long sessionId = creerSession();

        rouvrir(sessionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_OUVERTE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void rouvrirUneSessionInconnueRenvoie404() throws Exception {
        rouvrir(999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void laReouvertureNeModifiePasExpirationAt() throws Exception {
        // Session ouverte 3h avant « maintenant » : un recalcul depuis l'horloge serait détecté
        Long sessionId = creerSession();
        SessionEntity session = sessions.findById(sessionId).orElseThrow();
        Instant ouvertureOrigine = MAINTENANT_FIGE.minus(Duration.ofHours(3));
        Instant expirationOrigine = ouvertureOrigine.plus(Duration.ofMinutes(15));
        session.setOuvertureAt(ouvertureOrigine);
        session.setExpirationAt(expirationOrigine);
        entityManager.flush();

        cloturer(sessionId).andExpect(status().isOk());
        rouvrir(sessionId).andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        SessionEntity relue = sessions.findById(sessionId).orElseThrow();
        assertThat(relue.getExpirationAt()).isEqualTo(expirationOrigine); // RG15
        assertThat(relue.getOuvertureAt()).isEqualTo(ouvertureOrigine);
    }

    @Test
    void presenceManuelleRefuseePendantLaClotureAccepteeApresReouverture() throws Exception {
        Long sessionId = creerSession();
        EtudiantEntity etudiant = new EtudiantEntity();
        etudiant.setPromotion(promotions.findById(promotionId).orElseThrow());
        etudiant.setNom("Alice");
        Long etudiantId = etudiants.save(etudiant).getId();
        String corps = "{\"etudiantId\":" + etudiantId + "}";

        cloturer(sessionId).andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        rouvrir(sessionId).andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }
}
