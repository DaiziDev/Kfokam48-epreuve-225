package com.kfokam48.kfokam48.exercice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.relecture.RelectureEntity;
import com.kfokam48.kfokam48.relecture.RelectureRepository;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PromotionEntity;
import com.kfokam48.kfokam48.session.PromotionRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * EF6/EF7/EF8 : dépôt et remplacement du lien d'exercice, assignation du
 * relecteur. Les présents sont enregistrés par l'ajout manuel du formateur
 * (EF5) pour ne pas dépendre de la fenêtre du code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ExerciceControllerIntegrationTest.HorlogeFiguee.class)
class ExerciceControllerIntegrationTest {

    private static final String LIEN = "https://github.com/alice/exercice-algo";
    private static final String AUTRE_LIEN = "https://gitlab.com/alice/exercice-algo-v2";

    @TestConfiguration
    static class HorlogeFiguee {
        @Bean
        @Primary
        Clock horlogeFiguee() {
            return Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC);
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
    private ExerciceRepository exercices;

    @Autowired
    private RelectureRepository relectures;

    private Long promotionId;
    private Long alice;
    private Long boris;
    private Long chloe;
    private Long denis;
    private Long horsPromotion;

    @BeforeEach
    void preparer() {
        PromotionEntity promotion = promotions.save(promotion("KFOKAM48"));
        promotionId = promotion.getId();
        alice = etudiants.save(etudiant(promotion, "Alice")).getId();
        boris = etudiants.save(etudiant(promotion, "Boris")).getId();
        chloe = etudiants.save(etudiant(promotion, "Chloé")).getId();
        denis = etudiants.save(etudiant(promotion, "Denis")).getId();
        horsPromotion = etudiants.save(etudiant(promotions.save(promotion("KFOKAM49")), "Eva")).getId();
    }

    private PromotionEntity promotion(String nom) {
        PromotionEntity p = new PromotionEntity();
        p.setNom(nom);
        return p;
    }

    private EtudiantEntity etudiant(PromotionEntity promotion, String nom) {
        EtudiantEntity e = new EtudiantEntity();
        e.setPromotion(promotion);
        e.setNom(nom);
        return e;
    }

    private Long creerSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private void marquerPresents(Long sessionId, Long... etudiantIds) throws Exception {
        for (Long etudiantId : etudiantIds) {
            mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"etudiantId\":" + etudiantId + "}"))
                    .andExpect(status().isCreated());
        }
    }

    private ResultActions deposer(Long sessionId, Long etudiantId, String lien) throws Exception {
        return mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                        + ",\"lien\":\"" + lien + "\"}"));
    }

    private Long deposerEtRetournerId(Long sessionId, Long etudiantId) throws Exception {
        MvcResult resultat = deposer(sessionId, etudiantId, LIEN).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions remplacer(Long exerciceId, Long etudiantId, String lien) throws Exception {
        return mockMvc.perform(put("/api/exercices/" + exerciceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + etudiantId + ",\"lien\":\"" + lien + "\"}"));
    }

    private Long relecteurDe(Long exerciceId) {
        RelectureEntity relecture = relectures.findByExerciceId(exerciceId).orElseThrow();
        return relecture.getRelecteur().getId();
    }

    // --- EF6/EF8 : dépôt ---

    @Test
    void depotCreeUnExerciceEnAttenteDeRelectureSansExposerLeRelecteur() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.relecteurId").doesNotExist());
    }

    @Test
    void depotAssigneLeSeulAutrePresentCommeRelecteur() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);

        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        assertThat(relecteurDe(exerciceId)).isEqualTo(boris);
    }

    @Test
    void relecteurJamaisAuteurEtToujoursPresent() throws Exception {
        // Denis est de la promotion mais absent : il ne doit jamais être tiré (RG6)
        List<Long> presents = List.of(alice, boris, chloe);
        for (int tour = 0; tour < 5; tour++) {
            Long sessionId = creerSession();
            marquerPresents(sessionId, alice, boris, chloe);
            for (Long auteur : presents) {
                Long relecteur = relecteurDe(deposerEtRetournerId(sessionId, auteur));
                assertThat(relecteur).isNotEqualTo(auteur).isIn(presents); // RG4, RG6
            }
        }
    }

    @Test
    void depotSansAutrePresentRenvoie422() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice);

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("AUCUN_RELECTEUR_DISPONIBLE"))
                .andExpect(jsonPath("$.message").isString());
        assertThat(exercices.count()).isZero();
    }

    @Test
    void depotResteRefuseJusquAuPremierAutrePresentPuisAccepte() throws Exception {
        Long sessionId = creerSession();
        deposer(sessionId, alice, LIEN).andExpect(status().isUnprocessableContent());

        marquerPresents(sessionId, boris);

        deposer(sessionId, alice, LIEN).andExpect(status().isCreated());
    }

    @Test
    void deuxiemeDepotRenvoie409ExerciceDejaDepose() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        deposer(sessionId, alice, LIEN).andExpect(status().isCreated());

        deposer(sessionId, alice, AUTRE_LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void depotSurSessionClotureeRenvoie409() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void depotSurSessionInconnueRenvoie404() throws Exception {
        deposer(999999L, alice, LIEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void depotParEtudiantInconnuOuHorsPromotionRenvoie404() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);

        deposer(sessionId, 999999L, LIEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
        deposer(sessionId, horsPromotion, LIEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void depotParUnAuteurAbsentEstAccepte() throws Exception {
        // Aucune règle n'exige la présence de l'auteur (section 7)
        Long sessionId = creerSession();
        marquerPresents(sessionId, boris, chloe);

        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        assertThat(relecteurDe(exerciceId)).isIn(boris, chloe);
    }

    @Test
    void lienInvalideRenvoie400() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);

        for (String lien : List.of("pas une url", "github.com/alice", "ftp://serveur/exo",
                "javascript:alert(1)", "https://")) {
            deposer(sessionId, alice, lien)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
        }
    }

    @Test
    void champManquantRenvoie400ChampsRequis() throws Exception {
        mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":" + alice + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));
    }

    // --- EF7/RG12 : remplacement ---

    @Test
    void remplacementChangeLeLienEtGardeLeRelecteur() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        Long relecteurAvant = relecteurDe(exerciceId);

        remplacer(exerciceId, alice, AUTRE_LIEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciceId))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"));

        assertThat(exercices.findById(exerciceId).orElseThrow().getLien()).isEqualTo(AUTRE_LIEN);
        assertThat(relecteurDe(exerciceId)).isEqualTo(relecteurAvant);
    }

    @Test
    void remplacementPossibleApresClotureDeLaSession() throws Exception {
        // RG12 : indépendant de la clôture, contrairement au dépôt (RG11)
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());

        remplacer(exerciceId, alice, AUTRE_LIEN).andExpect(status().isOk());
    }

    @Test
    void remplacementApresRelectureRendueRenvoie409() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        // Le rendu de relecture (EF9) viendra avec son ticket : on pose l'état
        exercices.findById(exerciceId).orElseThrow().setStatut(ExerciceStatut.RELU);

        remplacer(exerciceId, alice, AUTRE_LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_COMMENCEE"));
        assertThat(exercices.findById(exerciceId).orElseThrow().getLien()).isEqualTo(LIEN);
    }

    @Test
    void remplacementParUnAutreEtudiantRenvoie403() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        remplacer(exerciceId, boris, AUTRE_LIEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NON_AUTEUR"));
    }

    @Test
    void remplacementExerciceInconnuRenvoie404() throws Exception {
        remplacer(999999L, alice, AUTRE_LIEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

    @Test
    void remplacementAvecLienInvalideRenvoie400() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        remplacer(exerciceId, alice, "pas une url")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }
}
