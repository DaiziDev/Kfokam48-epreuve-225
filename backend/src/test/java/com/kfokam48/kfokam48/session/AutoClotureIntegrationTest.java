package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/**
 * EF14/RG14 : une session OUVERTE passe à CLOTUREE 24h après expirationAt, sans
 * action du formateur. La règle est appelée directement (la tâche planifiée est
 * coupée en profil test), l'horloge est déplacée autour de la limite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(AutoClotureIntegrationTest.HorlogeDeTest.class)
class AutoClotureIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-09-25T10:00:00Z");
    /** expirationAt d'une session ouverte à T0 (RG1), puis + 24h (RG14). */
    private static final Instant LIMITE = T0.plus(Duration.ofMinutes(15)).plus(Duration.ofHours(24));

    @TestConfiguration
    static class HorlogeDeTest {
        final HorlogeMutable horloge = new HorlogeMutable(T0);

        @Bean
        @Primary
        Clock horlogeDeTest() {
            return horloge;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HorlogeDeTest horlogeDeTest;

    @Autowired
    private SessionService service;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private ApplicationContext contexte;

    private Long promotionId;

    @BeforeEach
    void preparer() {
        horlogeDeTest.horloge.avancerA(T0);
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("KFOKAM48");
        promotionId = promotions.save(promotion).getId();
    }

    /** Ouvre une session via l'API à l'heure courante de l'horloge de test. */
    private Long creerSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private SessionStatut statut(Long sessionId) {
        return sessions.findById(sessionId).orElseThrow().getStatut();
    }

    @Test
    void sessionEncoreOuverteUneSecondeAvantLaLimite() throws Exception {
        Long sessionId = creerSession();
        horlogeDeTest.horloge.avancerA(LIMITE.minusSeconds(1));

        assertThat(service.cloturerSessionsEchues()).isZero();
        assertThat(statut(sessionId)).isEqualTo(SessionStatut.OUVERTE);
    }

    @Test
    void sessionClotureeAutomatiquement24hApresExpiration() throws Exception {
        Long sessionId = creerSession();
        horlogeDeTest.horloge.avancerA(LIMITE);

        assertThat(service.cloturerSessionsEchues()).isEqualTo(1);
        assertThat(statut(sessionId)).isEqualTo(SessionStatut.CLOTUREE);
    }

    @Test
    void seulesLesSessionsEchuesSontCloturees() throws Exception {
        Long ancienne = creerSession();
        horlogeDeTest.horloge.avancerA(T0.plus(Duration.ofHours(12)));
        Long recente = creerSession();

        horlogeDeTest.horloge.avancerA(LIMITE);

        assertThat(service.cloturerSessionsEchues()).isEqualTo(1);
        assertThat(statut(ancienne)).isEqualTo(SessionStatut.CLOTUREE);
        assertThat(statut(recente)).isEqualTo(SessionStatut.OUVERTE);
    }

    @Test
    void uneSessionDejaClotureeNestPasRecomptee() throws Exception {
        Long sessionId = creerSession();
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());
        horlogeDeTest.horloge.avancerA(LIMITE);

        assertThat(service.cloturerSessionsEchues()).isZero();
        assertThat(statut(sessionId)).isEqualTo(SessionStatut.CLOTUREE);
    }

    @Test
    void reouvertureNeRepoussePasLeDelaiDe24h() throws Exception {
        // Clôture par erreur puis réouverture 1h après l'ouverture (RG15)
        Long sessionId = creerSession();
        horlogeDeTest.horloge.avancerA(T0.plus(Duration.ofHours(1)));
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/reouverture")).andExpect(status().isOk());

        // La limite reste celle d'origine, pas « réouverture + 24h »
        horlogeDeTest.horloge.avancerA(LIMITE);

        assertThat(service.cloturerSessionsEchues()).isEqualTo(1);
        assertThat(statut(sessionId)).isEqualTo(SessionStatut.CLOTUREE);
    }

    @Test
    void laTachePlanifieeEstCoupeeEnProfilTest() {
        assertThat(contexte.getBeansOfType(AutoClotureTache.class)).isEmpty();
    }
}
