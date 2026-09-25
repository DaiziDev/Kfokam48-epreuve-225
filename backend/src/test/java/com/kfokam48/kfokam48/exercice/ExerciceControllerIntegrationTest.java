package com.kfokam48.kfokam48.exercice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
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

    /** Les relecteurs de l'exercice, dans l'ordre des rangs. */
    private List<Long> relecteursDe(Long exerciceId) {
        return relectures.findByExerciceIdOrderByRangAsc(exerciceId).stream()
                .map(r -> r.getRelecteur().getId())
                .toList();
    }

    // --- EF6/EF8 : dépôt ---

    @Test
    void depotCreeUnExerciceEnAttenteDeRelectureSansExposerLeRelecteur() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.relecteurId").doesNotExist());
    }

    @Test
    void depotAssigneDeuxAutresPresentsCommeRelecteurs() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);

        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        assertThat(relecteursDe(exerciceId)).containsExactlyInAnyOrder(boris, chloe);
    }

    @Test
    void relecteurJamaisAuteurEtToujoursPresent() throws Exception {
        // Denis est de la promotion mais absent : il ne doit jamais être tiré (RG6)
        List<Long> presents = List.of(alice, boris, chloe);
        for (int tour = 0; tour < 5; tour++) {
            Long sessionId = creerSession();
            marquerPresents(sessionId, alice, boris, chloe);
            for (Long auteur : presents) {
                List<Long> relecteurs = relecteursDe(deposerEtRetournerId(sessionId, auteur));
                assertThat(relecteurs).hasSize(2).doesNotHaveDuplicates();
                assertThat(relecteurs).doesNotContain(auteur).isSubsetOf(presents); // RG4, RG6
            }
        }
    }

    @Test
    void chaqueExerciceAExactementDeuxRelecteursDistincts() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe, denis);

        for (Long auteur : List.of(alice, boris, chloe, denis)) {
            Long exerciceId = deposerEtRetournerId(sessionId, auteur);
            List<RelectureEntity> relecturesExercice = relectures.findByExerciceIdOrderByRangAsc(exerciceId);
            assertThat(relecturesExercice).hasSize(2); // RG5 révisé
            assertThat(relecturesExercice.get(0).getRang()).isEqualTo(1);
            assertThat(relecturesExercice.get(1).getRang()).isEqualTo(2);
            Set<Long> relecteurs = relecturesExercice.stream().map(r -> r.getRelecteur().getId())
                    .collect(Collectors.toSet());
            assertThat(relecteurs).hasSize(2); // distincts l'un de l'autre
            assertThat(relecteurs).doesNotContain(auteur); // distincts de l'auteur (RG4)
        }
    }

    @Test
    void laBaseRefuseDeuxRelecturesDuMemeRelecteurPourLeMemeExercice() throws Exception {
        // RG5 révisé, garanti aussi en base (uq_relecture_exercice_relecteur)
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        Long premierRelecteur = relectures.findByExerciceIdOrderByRangAsc(exerciceId).get(0)
                .getRelecteur().getId();
        RelectureEntity doublon = new RelectureEntity();
        doublon.setExercice(exercices.getReferenceById(exerciceId));
        doublon.setRelecteur(etudiants.getReferenceById(premierRelecteur));
        doublon.setRang(2);

        assertThatThrownBy(() -> relectures.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRefuseUnTroisiemeRangPourLeMemeExercice() throws Exception {
        // Exactement deux relectures : le rang 3 est refusé en base
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe, denis);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        RelectureEntity troisieme = new RelectureEntity();
        troisieme.setExercice(exercices.getReferenceById(exerciceId));
        troisieme.setRelecteur(etudiants.getReferenceById(denis));
        troisieme.setRang(3);

        assertThatThrownBy(() -> relectures.saveAndFlush(troisieme))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void depotSansDeuxAutresPresentsRenvoie422() throws Exception {
        // Seuls deux présents (l'auteur et un autre) : impossible d'assigner deux relecteurs
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris);

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("AUCUN_RELECTEUR_DISPONIBLE"))
                .andExpect(jsonPath("$.message").isString());
        assertThat(exercices.count()).isZero();
    }

    @Test
    void depotSansAucunAutrePresentRenvoie422() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice);

        deposer(sessionId, alice, LIEN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("AUCUN_RELECTEUR_DISPONIBLE"));
    }

    @Test
    void depotResteRefuseJusquaDeuxAutresPresentsPuisAccepte() throws Exception {
        Long sessionId = creerSession();
        deposer(sessionId, alice, LIEN).andExpect(status().isUnprocessableContent());

        marquerPresents(sessionId, boris);
        deposer(sessionId, alice, LIEN).andExpect(status().isUnprocessableContent());

        marquerPresents(sessionId, chloe);
        deposer(sessionId, alice, LIEN).andExpect(status().isCreated());
    }

    @Test
    void deuxiemeDepotRenvoie409ExerciceDejaDepose() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
        deposer(sessionId, alice, LIEN).andExpect(status().isCreated());

        deposer(sessionId, alice, AUTRE_LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void depotSurSessionClotureeRenvoie409() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
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
        marquerPresents(sessionId, alice, boris, chloe);

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
        marquerPresents(sessionId, boris, chloe, denis);

        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        assertThat(relecteursDe(exerciceId)).hasSize(2) // deux relecteurs tirés parmi les trois présents
                .isSubsetOf(List.of(boris, chloe, denis))
                .doesNotHaveDuplicates()
                .doesNotContain(alice);
    }

    @Test
    void lienInvalideRenvoie400() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);

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
    void remplacementChangeLeLienEtGardeLesRelecteurs() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe, denis);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        List<Long> relecteursAvant = relecteursDe(exerciceId);

        remplacer(exerciceId, alice, AUTRE_LIEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciceId))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"));

        assertThat(exercices.findById(exerciceId).orElseThrow().getLien()).isEqualTo(AUTRE_LIEN);
        assertThat(relecteursDe(exerciceId)).isEqualTo(relecteursAvant);
    }

    @Test
    void remplacementPossibleApresClotureDeLaSession() throws Exception {
        // RG12 : indépendant de la clôture, contrairement au dépôt (RG11)
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());

        remplacer(exerciceId, alice, AUTRE_LIEN).andExpect(status().isOk());
    }

    @Test
    void remplacementApresRelectureRendueRenvoie409() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);
        RelectureEntity rendue = relectures.findByExerciceIdOrderByRangAsc(exerciceId).get(0);
        rendue.setNote(15);
        rendue.setCommentaire("Déjà rendu.");
        rendue.setRendueAt(Instant.now());

        remplacer(exerciceId, alice, AUTRE_LIEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_COMMENCEE"));
        assertThat(exercices.findById(exerciceId).orElseThrow().getLien()).isEqualTo(LIEN);
    }

    @Test
    void remplacementParUnAutreEtudiantRenvoie403() throws Exception {
        Long sessionId = creerSession();
        marquerPresents(sessionId, alice, boris, chloe);
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
        marquerPresents(sessionId, alice, boris, chloe);
        Long exerciceId = deposerEtRetournerId(sessionId, alice);

        remplacer(exerciceId, alice, "pas une url")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }
}
