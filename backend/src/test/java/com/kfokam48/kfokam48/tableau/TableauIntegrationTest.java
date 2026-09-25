package com.kfokam48.kfokam48.tableau;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

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
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.relecture.RelectureEntity;
import com.kfokam48.kfokam48.relecture.RelectureRepository;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PromotionEntity;
import com.kfokam48.kfokam48.session.PromotionRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * EF16 : tableau de suivi. Scénario déterministe — chaque dépôt porte deux
 * relectures ; la relecture de rang 1 est rendue, celle de rang 2 jamais, donc
 * chaque chiffre du tableau est connu à l'avance :
 *
 * S1 (Alice, Boris, Chloé)  : Boris relit Alice 15 ; Chloé relit Boris 11 ;
 *                             les secondes relectures ne sont pas rendues.
 * S2 (Alice, Chloé*, Denis) : Chloé relit Alice 12.   (* présence par code)
 * S3 (Alice, Denis, Boris)  : Denis relit Alice 14.
 * Fanny : aucune activité. Autre promotion : Eva, Gaston et Hélène, activité isolée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TableauIntegrationTest.HorlogeFiguee.class)
class TableauIntegrationTest {

    private static final Set<String> CHAMPS_DU_CONTRAT = Set.of("etudiantId", "nom", "presences",
            "exercicesDeposes", "moyenne", "moyenneProvisoire", "relecturesEnAttente");

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
    private RelectureRepository relectures;

    private Long promotionId;
    private Long autrePromotionId;
    private Long alice;
    private Long boris;
    private Long chloe;
    private Long denis;
    private Long fanny;
    private Long eva;
    private Long gaston;
    private Long helene;

    @BeforeEach
    void preparer() {
        PromotionEntity promotion = promotions.save(promotion("KFOKAM48"));
        promotionId = promotion.getId();
        alice = etudiants.save(etudiant(promotion, "Alice")).getId();
        boris = etudiants.save(etudiant(promotion, "Boris")).getId();
        chloe = etudiants.save(etudiant(promotion, "Chloé")).getId();
        denis = etudiants.save(etudiant(promotion, "Denis")).getId();
        fanny = etudiants.save(etudiant(promotion, "Fanny")).getId();

        PromotionEntity autre = promotions.save(promotion("KFOKAM49"));
        autrePromotionId = autre.getId();
        eva = etudiants.save(etudiant(autre, "Eva")).getId();
        gaston = etudiants.save(etudiant(autre, "Gaston")).getId();
        helene = etudiants.save(etudiant(autre, "Hélène")).getId();
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

    private JsonNode lire(MvcResult resultat) throws Exception {
        return objectMapper.readTree(resultat.getResponse().getContentAsString());
    }

    private JsonNode creerSession(Long promotion) throws Exception {
        return lire(mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotion + "}"))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void presenceManuelle(Long sessionId, Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
    }

    private void presenceParCode(String code, Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
    }

    private Long deposer(Long sessionId, Long auteur) throws Exception {
        return lire(mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + auteur
                        + ",\"lien\":\"https://github.com/kfokam48/exo\"}"))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
    }

    private Long rendre(Long exerciceId, Long relecteurId, int note) throws Exception {
        Long relectureId = relectures.findByExerciceIdOrderByRangAsc(exerciceId).stream()
                .filter(r -> r.getRelecteur().getId().equals(relecteurId)).findFirst().orElseThrow().getId();
        mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + relecteurId + ",\"note\":" + note + ",\"commentaire\":\"Relu.\"}"))
                .andExpect(status().isOk());
        return relectureId;
    }

    private Long relectureDe(Long exerciceId, Long relecteurId) {
        return relectures.findByExerciceIdOrderByRangAsc(exerciceId).stream()
                .filter(r -> r.getRelecteur().getId().equals(relecteurId))
                .findFirst().orElseThrow().getId();
    }

    private void rendreRelecture(Long relectureId, Long relecteurId, int note) throws Exception {
        mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":" + relecteurId + ",\"note\":" + note
                        + ",\"commentaire\":\"Relu.\"}"))
                .andExpect(status().isOk());
    }

    /** Joue le scénario décrit en tête de classe. */
    private void jouerLeScenario() throws Exception {
        Long s1 = creerSession(promotionId).get("id").asLong();
        presenceManuelle(s1, alice);
        presenceManuelle(s1, boris);
        presenceManuelle(s1, chloe);

        Long exerciceAliceS1 = deposer(s1, alice);
        Long exerciceBorisS1 = deposer(s1, boris);
        deposer(s1, chloe); // ses secondes relectures ne seront pas rendues

        // Rang 1 rendu sur chaque exercice ; les secondes relectures jamais.
        rendreRelecture(relectureDe(exerciceAliceS1, boris), boris, 15);
        rendreRelecture(relectureDe(exerciceBorisS1, chloe), chloe, 11);
        rendreRelecture(relectureDe(exerciceAliceS1, chloe), chloe, 12);

        JsonNode s2 = creerSession(promotionId);
        presenceManuelle(s2.get("id").asLong(), alice);
        presenceParCode(s2.get("code").asString(), chloe);
        presenceManuelle(s2.get("id").asLong(), denis);

        Long exerciceAliceS2 = deposer(s2.get("id").asLong(), alice);
        rendreRelecture(relectureDe(exerciceAliceS2, chloe), chloe, 12);

        Long s3 = creerSession(promotionId).get("id").asLong();
        presenceManuelle(s3, alice);
        presenceManuelle(s3, denis);
        presenceManuelle(s3, boris);

        Long exerciceAliceS3 = deposer(s3, alice);
        rendreRelecture(relectureDe(exerciceAliceS3, denis), denis, 14);

        Long autre = creerSession(autrePromotionId).get("id").asLong();
        presenceManuelle(autre, eva);
        presenceManuelle(autre, gaston);
        presenceManuelle(autre, helene);

        Long exerciceEva = deposer(autre, eva);
        rendreRelecture(relectureDe(exerciceEva, gaston), gaston, 20);
    }

    @Test
    void leTableauAfficheLesChiffresAttendusParEtudiant() throws Exception {
        jouerLeScenario();

        mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                // Alice : 3 présences, 3 dépôts, ((15+12)/2 + 12 + 14)/3 = 13.166… → 13.17,
                // 2 relectures en attente (rang 2 des sessions S1 et S3)
                .andExpect(jsonPath("$[0].etudiantId").value(alice))
                .andExpect(jsonPath("$[0].nom").value("Alice"))
                .andExpect(jsonPath("$[0].presences").value(3))
                .andExpect(jsonPath("$[0].exercicesDeposes").value(3))
                .andExpect(jsonPath("$[0].moyenne").value(13.17))
                .andExpect(jsonPath("$[0].moyenneProvisoire").value(true))
                .andExpect(jsonPath("$[0].relecturesEnAttente").value(2))
                // Boris : exercice relu par Chloé (11, provisoire) → moyenne 11 (RG10 : rendue seulement)
                .andExpect(jsonPath("$[1].etudiantId").value(boris))
                .andExpect(jsonPath("$[1].presences").value(2))
                .andExpect(jsonPath("$[1].exercicesDeposes").value(1))
                .andExpect(jsonPath("$[1].moyenne").value(11.0))
                .andExpect(jsonPath("$[1].moyenneProvisoire").value(true))
                .andExpect(jsonPath("$[1].relecturesEnAttente").value(2))
                // Chloé : présente par code (source ETUDIANT), 3 relectures rendues, aucune déposée
                .andExpect(jsonPath("$[2].etudiantId").value(chloe))
                .andExpect(jsonPath("$[2].presences").value(2))
                .andExpect(jsonPath("$[2].exercicesDeposes").value(1))
                .andExpect(jsonPath("$[2].relecturesEnAttente").value(0))
                .andExpect(jsonPath("$[3].etudiantId").value(denis))
                .andExpect(jsonPath("$[3].presences").value(2))
                // Fanny : aucune activité, figure quand même avec des zéros
                .andExpect(jsonPath("$[4].etudiantId").value(fanny))
                .andExpect(jsonPath("$[4].presences").value(0))
                .andExpect(jsonPath("$[4].exercicesDeposes").value(0))
                .andExpect(jsonPath("$[4].moyenne").isEmpty())
                .andExpect(jsonPath("$[4].relecturesEnAttente").value(0));
    }

    @Test
    void lesAutresPromotionsNeFuientPasDansLeTableau() throws Exception {
        jouerLeScenario();

        mockMvc.perform(get("/api/tableau").param("promotionId", autrePromotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].etudiantId").value(eva))
                .andExpect(jsonPath("$[0].presences").value(1))
                .andExpect(jsonPath("$[0].moyenne").value(20.0))
                .andExpect(jsonPath("$[1].etudiantId").value(gaston))
                .andExpect(jsonPath("$[1].relecturesEnAttente").value(0))
                .andExpect(jsonPath("$[2].etudiantId").value(helene));
    }

    @Test
    void chaqueLigneContientExactementLesChampsDuContrat() throws Exception {
        jouerLeScenario();

        JsonNode lignes = lire(mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                .andReturn());
        for (JsonNode ligne : lignes) {
            // moyenne présente même quand elle est nulle : le contrat la déclare requise et nullable
            assertThat(new HashSet<>(ligne.propertyNames())).isEqualTo(CHAMPS_DU_CONTRAT);
        }
    }

    @Test
    void promotionSansEtudiantRenvoieUneListeVide() throws Exception {
        Long vide = promotions.save(promotion("VIDE")).getId();

        mockMvc.perform(get("/api/tableau").param("promotionId", vide.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void promotionIdAbsentOuInvalideRenvoie400() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));
        mockMvc.perform(get("/api/tableau").param("promotionId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
    }
}
