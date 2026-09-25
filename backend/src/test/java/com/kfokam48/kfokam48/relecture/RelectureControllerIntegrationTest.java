package com.kfokam48.kfokam48.relecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.kfokam48.kfokam48.exercice.ExerciceRepository;
import com.kfokam48.kfokam48.exercice.ExerciceStatut;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PromotionEntity;
import com.kfokam48.kfokam48.session.PromotionRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * EF9/EF10/EF11 : rendu de la relecture par le relecteur assigné. Avec deux
 * présents (Alice, Boris), l'assignation est déterministe : Boris relit
 * l'exercice d'Alice, Alice celui de Boris.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelectureControllerIntegrationTest.HorlogeFiguee.class)
class RelectureControllerIntegrationTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T10:00:00Z");
    private static final String LIEN = "https://github.com/alice/exercice-algo";

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
    private ExerciceRepository exercices;

    @Autowired
    private RelectureRepository relectures;

    private Long promotionId;
    private Long alice;
    private Long boris;
    private Long chloe;
    private Long denis;
    private Long sessionId;
    /** Exercice d'Alice, relu par Boris et Chloé. */
    private Long exerciceAlice;
    /** Relecture de rang 1 de l'exercice d'Alice. */
    private Long relectureParBoris;

    @BeforeEach
    void preparer() throws Exception {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("KFOKAM48");
        promotion = promotions.save(promotion);
        promotionId = promotion.getId();
        alice = etudiants.save(etudiant(promotion, "Alice")).getId();
        boris = etudiants.save(etudiant(promotion, "Boris")).getId();
        chloe = etudiants.save(etudiant(promotion, "Chloé")).getId();
        denis = etudiants.save(etudiant(promotion, "Denis")).getId();

        sessionId = creerSession();
        marquerPresent(alice);
        marquerPresent(boris);
        marquerPresent(chloe);
        exerciceAlice = deposer(alice);
        List<RelectureEntity> relecturesAlice = relectures.findByExerciceIdOrderByRangAsc(exerciceAlice);
        relectureParBoris = relecturesAlice.stream()
                .filter(r -> r.getRelecteur().getId().equals(boris))
                .findFirst().orElseThrow().getId();
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

    private void marquerPresent(Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
    }

    private Long deposer(Long auteur) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + auteur
                        + ",\"lien\":\"" + LIEN + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).get("id").asLong();
    }

    /** note est passée brute pour pouvoir envoyer 12.5, -1, etc. */
    private ResultActions rendre(Long relectureId, Long relecteurId, String note, String commentaire)
            throws Exception {
        return mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + relecteurId + ",\"note\":" + note
                        + ",\"commentaire\":\"" + commentaire + "\"}"));
    }

    private ExerciceStatut statutExercice(Long exerciceId) {
        return exercices.findById(exerciceId).orElseThrow().getStatut();
    }

    // --- EF9 : rendu nominal ---

    @Test
    void noteEntiereAvecCommentairePasseLExerciceARelu() throws Exception {
        rendre(relectureParBoris, boris, "15", "Bonne structure, tests à compléter.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relectureParBoris))
                .andExpect(jsonPath("$.exerciceId").value(exerciceAlice))
                .andExpect(jsonPath("$.note").value(15))
                .andExpect(jsonPath("$.commentaire").value("Bonne structure, tests à compléter."))
                .andExpect(jsonPath("$.rendueAt").isString());

        assertThat(statutExercice(exerciceAlice)).isEqualTo(ExerciceStatut.EN_ATTENTE_RELECTURE);
        RelectureEntity relecture = relectures.findById(relectureParBoris).orElseThrow();
        assertThat(relecture.getNote()).isEqualTo(15);
        assertThat(relecture.getRendueAt()).isEqualTo(MAINTENANT);
    }

    @Test
    void lesBornes0Et20SontAcceptees() throws Exception {
        Long exerciceBoris = deposer(boris); // relu par Alice et Chloé
        List<RelectureEntity> relecturesBoris = relectures.findByExerciceIdOrderByRangAsc(exerciceBoris);
        Long relectureParAlice = relecturesBoris.stream()
                .filter(r -> r.getRelecteur().getId().equals(alice)).findFirst().orElseThrow().getId();
        Long relectureParChloe = relecturesBoris.stream()
                .filter(r -> r.getRelecteur().getId().equals(chloe)).findFirst().orElseThrow().getId();

        rendre(relectureParBoris, boris, "0", "Hors sujet.").andExpect(status().isOk());
        rendre(relectureParAlice, alice, "20", "Parfait.").andExpect(status().isOk());
        rendre(relectureParChloe, chloe, "10", "Correct.").andExpect(status().isOk());
    }

    @Test
    void uneNoteEntiereEcriteEnDecimalEstAcceptee() throws Exception {
        rendre(relectureParBoris, boris, "12.0", "Correct.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(12));
    }

    // --- EF9/RG8 : NOTE_INVALIDE ---

    @Test
    void noteHorsBornesOuNonEntiereRenvoie400NoteInvalide() throws Exception {
        for (String note : List.of("-1", "21", "12.5", "100000000000000000000")) {
            rendre(relectureParBoris, boris, note, "Commentaire.")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"))
                    .andExpect(jsonPath("$.message").isString());
        }
        // 12.5 n'a pas été tronqué en 12 : rien n'est enregistré
        assertThat(statutExercice(exerciceAlice)).isEqualTo(ExerciceStatut.EN_ATTENTE_RELECTURE);
        assertThat(relectures.findById(relectureParBoris).orElseThrow().getRendueAt()).isNull();
    }

    // --- Note provisoire et moyenne finale (exigence révisée) ---

    @Test
    void apresUnSeulRenduLaNoteEstProvisoire() throws Exception {
        rendre(relectureParBoris, boris, "15", "Premier avis.").andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.note").value(15))
                .andExpect(jsonPath("$.noteProvisoire").value(true));
    }

    @Test
    void apresDeuxRendusLaMoyenneEstLaNoteFinale() throws Exception {
        // Relecture de rang 2 de l'exercice d'Alice, portée par l'autre relecteur
        Long relectureDeux = relectures.findByExerciceIdOrderByRangAsc(exerciceAlice).stream()
                .filter(r -> !r.getRelecteur().getId().equals(boris)).findFirst().orElseThrow().getId();
        Long secondRelecteur = relectures.findById(relectureDeux).orElseThrow().getRelecteur().getId();

        rendre(relectureParBoris, boris, "12", "Premier avis.").andExpect(status().isOk());
        rendre(relectureDeux, secondRelecteur, "15", "Second avis.").andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.note").value(13.5))
                .andExpect(jsonPath("$.noteProvisoire").value(false));
    }

    @Test
    void laMoyenneEntiereEstExposeeSansDecimale() throws Exception {
        Long relectureDeux = relectures.findByExerciceIdOrderByRangAsc(exerciceAlice).stream()
                .filter(r -> !r.getRelecteur().getId().equals(boris)).findFirst().orElseThrow().getId();
        Long secondRelecteur = relectures.findById(relectureDeux).orElseThrow().getRelecteur().getId();

        rendre(relectureParBoris, boris, "10", "Premier avis.").andExpect(status().isOk());
        rendre(relectureDeux, secondRelecteur, "16", "Second avis.").andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(13))
                .andExpect(jsonPath("$.noteProvisoire").value(false));
    }

    @Test
    void noteOuCommentaireManquantRenvoie400ChampsRequis() throws Exception {
        mockMvc.perform(post("/api/relectures/" + relectureParBoris)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + boris + ",\"commentaire\":\"Sans note.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));

        rendre(relectureParBoris, boris, "15", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));
    }

    @Test
    void commentaireTropLongRenvoie400EtPas500() throws Exception {
        rendre(relectureParBoris, boris, "15", "x".repeat(4001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENTAIRE_INVALIDE"));
    }

    // --- EF10/RG4 : AUTO_RELECTURE ---

    @Test
    void tentativeSurSonPropreExerciceRenvoie403AutoRelecture() throws Exception {
        rendre(relectureParBoris, alice, "20", "Je me note moi-même.")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"))
                .andExpect(jsonPath("$.message").isString());

        assertThat(statutExercice(exerciceAlice)).isEqualTo(ExerciceStatut.EN_ATTENTE_RELECTURE);
    }

    @Test
    void unEtudiantNonAssigneNePeutPasRendreLaRelecture() throws Exception {
        // Denis est absent de la session : il ne peut jamais être relecteur
        rendre(relectureParBoris, denis, "10", "Je ne suis pas le relecteur.")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RELECTEUR_NON_ASSIGNE"));
    }

    // --- EF11/RG9 : RELECTURE_DEJA_RENDUE, note verrouillée ---

    @Test
    void deuxiemeSoumissionRenvoie409EtLaNoteResteVerrouillee() throws Exception {
        rendre(relectureParBoris, boris, "15", "Premier avis.").andExpect(status().isOk());

        rendre(relectureParBoris, boris, "5", "Je change d'avis.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));

        RelectureEntity relecture = relectures.findById(relectureParBoris).orElseThrow();
        assertThat(relecture.getNote()).isEqualTo(15);
        assertThat(relecture.getCommentaire()).isEqualTo("Premier avis.");
    }

    @Test
    void apresLeRenduLeLienNePeutPlusEtreRemplace() throws Exception {
        // Bout en bout avec EF7 : le rendu réel déclenche RELECTURE_DEJA_COMMENCEE
        rendre(relectureParBoris, boris, "15", "Relu.").andExpect(status().isOk());

        mockMvc.perform(put("/api/exercices/" + exerciceAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + alice + ",\"lien\":\"https://github.com/alice/v2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_COMMENCEE"));
    }

    // --- Cas limites ---

    @Test
    void relectureInconnueRenvoie404() throws Exception {
        rendre(999999L, boris, "15", "Commentaire.")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    @Test
    void identifiantNonNumeriqueRenvoie400EtPas500() throws Exception {
        mockMvc.perform(post("/api/relectures/abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + boris + ",\"note\":15,\"commentaire\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
    }

    @Test
    void leRenduResteSoupleApresClotureDeLaSession() throws Exception {
        // Aucune règle ne lie le rendu à l'état de la session (section 7)
        mockMvc.perform(patch("/api/sessions/" + sessionId + "/cloture")).andExpect(status().isOk());

        rendre(relectureParBoris, boris, "14", "Relu après la clôture.").andExpect(status().isOk());
    }

    // --- EF9 : liste des relectures du relecteur ---

    @Test
    void leRelecteurVoitSesRelecturesAFaireEtRendues() throws Exception {
        mockMvc.perform(get("/api/relectures").param("relecteurId", boris.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(relectureParBoris))
                .andExpect(jsonPath("$[0].exerciceId").value(exerciceAlice))
                .andExpect(jsonPath("$[0].exerciceLien").value(LIEN))
                .andExpect(jsonPath("$[0].rendue").value(false));

        rendre(relectureParBoris, boris, "15", "Relu.").andExpect(status().isOk());

        mockMvc.perform(get("/api/relectures").param("relecteurId", boris.toString()))
                .andExpect(jsonPath("$[0].rendue").value(true));
    }

    @Test
    void unEtudiantSansRelectureRecoitUneListeVide() throws Exception {
        // Denis, absent de la session, ne peut porter aucune relecture
        mockMvc.perform(get("/api/relectures").param("relecteurId", denis.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listeAvecRelecteurInconnuOuAbsentRenvoie4xx() throws Exception {
        mockMvc.perform(get("/api/relectures").param("relecteurId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));

        mockMvc.perform(get("/api/relectures"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));
    }
}
