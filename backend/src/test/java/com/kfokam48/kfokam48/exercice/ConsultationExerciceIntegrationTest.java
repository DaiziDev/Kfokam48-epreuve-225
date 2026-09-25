package com.kfokam48.kfokam48.exercice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.relecture.RelectureRepository;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PromotionEntity;
import com.kfokam48.kfokam48.session.PromotionRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * EF12/RG7 : l'étudiant relu consulte son exercice — note et commentaire une
 * fois la relecture rendue, jamais l'identité du relecteur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultationExerciceIntegrationTest {

    private static final String LIEN = "https://github.com/alice/exercice-algo";
    /** Seuls champs autorisés par le contrat : tout ajout doit être une décision explicite. */
    private static final Set<String> CHAMPS_DU_CONTRAT = Set.of("id", "lien", "statut", "note", "noteProvisoire", "commentaires");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private EtudiantRepository etudiants;

    @Autowired
    private RelectureRepository relectures;

    private Long alice;
    private Long boris;
    private Long chloe;
    /** Exercice d'Alice, relu par Boris (seul autre présent). */
    private Long exerciceAlice;

    @BeforeEach
    void preparer() throws Exception {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("KFOKAM48");
        promotion = promotions.save(promotion);
        alice = etudiants.save(etudiant(promotion, "Alice")).getId();
        boris = etudiants.save(etudiant(promotion, "Boris Relecteur")).getId();
        chloe = etudiants.save(etudiant(promotion, "Chloé Relectrice")).getId();

        MvcResult session = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotion.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long sessionId = lire(session).get("id").asLong();

        for (Long etudiantId : new Long[] { alice, boris, chloe }) {
            mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"etudiantId\":" + etudiantId + "}"))
                    .andExpect(status().isCreated());
        }

        MvcResult depot = mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + alice + ",\"lien\":\"" + LIEN + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        exerciceAlice = lire(depot).get("id").asLong();
    }

    private EtudiantEntity etudiant(PromotionEntity promotion, String nom) {
        EtudiantEntity e = new EtudiantEntity();
        e.setPromotion(promotion);
        e.setNom(nom);
        return e;
    }

    private JsonNode lire(MvcResult resultat) throws Exception {
        return objectMapper.readTree(resultat.getResponse().getContentAsString());
    }

    private void rendreRelecture(int note, String commentaire) throws Exception {
        var relecture = relectures.findByExerciceIdOrderByRangAsc(exerciceAlice).stream()
                .filter(r -> r.getRendueAt() == null).findFirst().orElseThrow();
        Long relectureId = relecture.getId();
        mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + relecture.getRelecteur().getId() + ",\"note\":" + note
                        + ",\"commentaire\":\"" + commentaire + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void avantLaRelectureNoteEtCommentaireSontNuls() throws Exception {
        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciceAlice))
                .andExpect(jsonPath("$.lien").value(LIEN))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.note").isEmpty())
                .andExpect(jsonPath("$.noteProvisoire").value(false))
                .andExpect(jsonPath("$.commentaires").isEmpty());
    }

    @Test
    void apresUneRelectureLEtudiantVoitUneNoteProvisoireEtLeCommentaireAnonyme() throws Exception {
        rendreRelecture(15, "Bonne structure, tests à compléter.");

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.note").value(15))
                .andExpect(jsonPath("$.noteProvisoire").value(true))
                .andExpect(jsonPath("$.commentaires[0]").value("Bonne structure, tests à compléter."));
    }

    @Test
    void apresDeuxRelecturesLaNoteRetenueEstLeurMoyenneEtElleNestPlusProvisoire() throws Exception {
        rendreRelecture(15, "Premier retour.");
        rendreRelecture(16, "Second retour.");

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.note").value(15.5))
                .andExpect(jsonPath("$.noteProvisoire").value(false))
                .andExpect(jsonPath("$.commentaires.length()").value(2));
    }

    @Test
    void laReponseNeContientAucunChampIdentifiantLeRelecteur() throws Exception {
        rendreRelecture(15, "Relu.");

        MvcResult resultat = mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(status().isOk())
                .andReturn();

        // RG7 : exactement les champs du contrat — un champ ajouté plus tard fait échouer ce test
        JsonNode corps = lire(resultat);
        Set<String> champs = new HashSet<>(corps.propertyNames());
        assertThat(champs).isEqualTo(CHAMPS_DU_CONTRAT);

        // Et aucune valeur ne trahit les relecteurs (nom ou identifiant)
        String brut = resultat.getResponse().getContentAsString();
        assertThat(brut).doesNotContainIgnoringCase("relecteur").doesNotContain("Boris").doesNotContain("Chloé");
        assertThat(corps.get("id").asLong()).isNotEqualTo(boris).isNotEqualTo(chloe);
    }

    @Test
    void leLienAfficheEstLeDernierDepose() throws Exception {
        String nouveauLien = "https://gitlab.com/alice/exercice-algo-v2";
        mockMvc.perform(put("/api/exercices/" + exerciceAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + alice + ",\"lien\":\"" + nouveauLien + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceAlice))
                .andExpect(jsonPath("$.lien").value(nouveauLien));
    }

    @Test
    void mesExercicesListeCeuxDeLEtudiantSansRienSurLeRelecteur() throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/exercices").param("etudiantId", alice.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(exerciceAlice))
                .andExpect(jsonPath("$[0].sessionTitre").value("Algorithmique"))
                .andExpect(jsonPath("$[0].lien").value(LIEN))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE_RELECTURE"))
                .andReturn();

        // RG7 aussi sur la liste : champs exacts, aucune trace du relecteur
        JsonNode ligne = lire(resultat).get(0);
        assertThat(new HashSet<>(ligne.propertyNames()))
                .isEqualTo(Set.of("id", "sessionId", "sessionTitre", "lien", "statut"));
        assertThat(resultat.getResponse().getContentAsString()).doesNotContain("Boris");

        mockMvc.perform(get("/api/exercices").param("etudiantId", boris.toString()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/exercices").param("etudiantId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void exerciceInconnuRenvoie404() throws Exception {
        mockMvc.perform(get("/api/exercices/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void identifiantNonNumeriqueRenvoie400() throws Exception {
        mockMvc.perform(get("/api/exercices/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
    }
}
