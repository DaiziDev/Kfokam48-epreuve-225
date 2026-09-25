package com.kfokam48.kfokam48.relecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.exercice.ExerciceRepository;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PresenceRepository;
import com.kfokam48.kfokam48.session.PromotionEntity;
import com.kfokam48.kfokam48.session.PromotionRepository;
import com.kfokam48.kfokam48.session.SessionEntity;
import com.kfokam48.kfokam48.session.SessionRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Exigence révisée : chaque exercice est attribué à deux relecteurs distincts
 * de son auteur et l'un de l'autre (RG4, RG5 révisés). Note provisoire après
 * un seul rendu, moyenne finale après les deux (critères d'acceptation).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Rollback après chaque test : sans lui, ses sessions restaient en base et
// faussaient les comptages de AutoClotureIntegrationTest (base H2 partagée)
@Transactional
@Import(AttributionDeuxRelecteursIntegrationTest.HorlogeFiguee.class)
class AttributionDeuxRelecteursIntegrationTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T10:00:00Z");

    @TestConfiguration
    static class HorlogeFiguee {
        @Bean
        @Primary
        Clock horlogeFiguee() {
            return Clock.fixed(MAINTENANT, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private EtudiantRepository etudiants;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private PresenceRepository presences;

    @Autowired
    private ExerciceRepository exercices;

    @Autowired
    private RelectureRepository relectures;

    @Test
    void unNouvelExerciceObtientDeuxRelecturesDeDeuxEtudiantsDistinctsHorsAuteur() throws Exception {
        Long promotionId = creerPromotion();
        Long alice = creerEtudiant(promotionId, "Alice");
        Long boris = creerEtudiant(promotionId, "Boris");
        Long chloe = creerEtudiant(promotionId, "Chloé");
        Long denis = creerEtudiant(promotionId, "Denis");

        Long sessionId = creerSession(promotionId);
        marquerPresent(sessionId, alice);
        marquerPresent(sessionId, boris);
        marquerPresent(sessionId, chloe);
        marquerPresent(sessionId, denis);

        Long exerciceId = deposer(sessionId, alice);

        var relecturesExercice = relectures.findByExerciceIdOrderByRangAsc(exerciceId);
        assertThat(relecturesExercice).hasSize(2);
        assertThat(relecturesExercice.get(0).getRang()).isEqualTo(1);
        assertThat(relecturesExercice.get(1).getRang()).isEqualTo(2);
        Long relecteur1 = relecturesExercice.get(0).getRelecteur().getId();
        Long relecteur2 = relecturesExercice.get(1).getRelecteur().getId();
        // Distincts l'un de l'autre (RG5 révisé) et de l'auteur (RG4)
        assertThat(relecteur1).isNotEqualTo(relecteur2).isNotEqualTo(alice);
        assertThat(relecteur2).isNotEqualTo(alice);
        // Tirés parmi les présents (RG6)
        assertThat(relecteur1).isIn(boris, chloe, denis);
        assertThat(relecteur2).isIn(boris, chloe, denis);
    }

    @Test
    void depotRefuse422SansDeuxCandidatsEligibles() throws Exception {
        Long promotionId = creerPromotion();
        Long alice = creerEtudiant(promotionId, "Alice");
        Long boris = creerEtudiant(promotionId, "Boris");

        Long sessionId = creerSession(promotionId);
        marquerPresent(sessionId, alice);
        marquerPresent(sessionId, boris);

        // Un seul autre présent : impossible d'assigner deux relecteurs distincts
        deposerBrut(sessionId, alice)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("AUCUN_RELECTEUR_DISPONIBLE"))
                .andExpect(jsonPath("$.message").isString());

        assertThat(exercices.countBySessionId(sessionId)).isZero();
        assertThat(relectures.countByExercice_SessionId(sessionId)).isZero();

        // Un second présent arrive : le dépôt redevient possible (refus transitoire)
        marquerPresent(sessionId, creerEtudiant(promotionId, "Chloé"));
        deposerBrut(sessionId, alice).andExpect(status().isCreated());
    }

    @Test
    void laNoteProvisoireDevientLaMoyenneFinaleApresLeSecondRendu() throws Exception {
        Long promotionId = creerPromotion();
        Long alice = creerEtudiant(promotionId, "Alice");
        Long boris = creerEtudiant(promotionId, "Boris");
        Long chloe = creerEtudiant(promotionId, "Chloé");

        Long sessionId = creerSession(promotionId);
        marquerPresent(sessionId, alice);
        marquerPresent(sessionId, boris);
        marquerPresent(sessionId, chloe);

        Long exerciceId = deposer(sessionId, alice);
        var relecturesExercice = relectures.findByExerciceIdOrderByRangAsc(exerciceId);
        Long relecture1 = relecturesExercice.get(0).getId();
        Long relecteur1 = relecturesExercice.get(0).getRelecteur().getId();
        Long relecture2 = relecturesExercice.get(1).getId();
        Long relecteur2 = relecturesExercice.get(1).getRelecteur().getId();

        // Aucun rendu : pas de note
        consulter(exerciceId)
                .andExpect(jsonPath("$.note").isEmpty())
                .andExpect(jsonPath("$.noteProvisoire").value(false))
                .andExpect(jsonPath("$.commentaires").isEmpty());

        // Premier rendu : note provisoire = la note rendue
        rendre(relecture1, relecteur1, 12, "Premier avis.");
        consulter(exerciceId)
                .andExpect(jsonPath("$.note").value(12))
                .andExpect(jsonPath("$.noteProvisoire").value(true))
                .andExpect(jsonPath("$.commentaires.length()").value(1));

        // Second rendu : la moyenne devient la note finale, plus provisoire
        rendre(relecture2, relecteur2, 15, "Second avis.");
        consulter(exerciceId)
                .andExpect(jsonPath("$.note").value(13.5))
                .andExpect(jsonPath("$.noteProvisoire").value(false))
                .andExpect(jsonPath("$.commentaires.length()").value(2));
    }

    private org.springframework.test.web.servlet.ResultActions consulter(Long exerciceId) throws Exception {
        return mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk());
    }

    private void rendre(Long relectureId, Long relecteurId, int note, String commentaire) throws Exception {
        mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + relecteurId + ",\"note\":" + note
                        + ",\"commentaire\":\"" + commentaire + "\"}"))
                .andExpect(status().isOk());
    }

    private Long creerPromotion() {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("KFOKAM48-" + System.nanoTime());
        return promotions.save(promotion).getId();
    }

    private Long creerEtudiant(Long promotionId, String nom) {
        EtudiantEntity etudiant = new EtudiantEntity();
        etudiant.setPromotion(promotions.getReferenceById(promotionId));
        etudiant.setNom(nom);
        return etudiants.save(etudiant).getId();
    }

    private Long creerSession(Long promotionId) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private void marquerPresent(Long sessionId, Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
    }

    private Long deposer(Long sessionId, Long auteurId) throws Exception {
        MvcResult resultat = deposerBrut(sessionId, auteurId).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions deposerBrut(Long sessionId, Long auteurId)
            throws Exception {
        return mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + auteurId
                        + ",\"lien\":\"https://github.com/alice/exercice-algo\"}"));
    }
}
