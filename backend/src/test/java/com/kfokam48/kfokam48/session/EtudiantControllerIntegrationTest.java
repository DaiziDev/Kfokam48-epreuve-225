package com.kfokam48.kfokam48.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Liste des étudiants d'une promotion (sélecteur d'identité du frontend). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EtudiantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private EtudiantRepository etudiants;

    private EtudiantEntity etudiant(PromotionEntity promotion, String nom) {
        EtudiantEntity e = new EtudiantEntity();
        e.setPromotion(promotion);
        e.setNom(nom);
        return etudiants.save(e);
    }

    @Test
    void listeLesEtudiantsDeLaPromotionTriesParNom() throws Exception {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("TEST");
        promotion = promotions.save(promotion);
        PromotionEntity autre = new PromotionEntity();
        autre.setNom("AUTRE");
        autre = promotions.save(autre);

        Long zoe = etudiant(promotion, "Zoé").getId();
        Long adam = etudiant(promotion, "Adam").getId();
        etudiant(autre, "Hors promotion");

        mockMvc.perform(get("/api/etudiants").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(adam))
                .andExpect(jsonPath("$[0].nom").value("Adam"))
                .andExpect(jsonPath("$[1].id").value(zoe));
    }

    @Test
    void laPromotionDeDemonstrationContientSesEtudiants() throws Exception {
        // V2 + V6 : le frontend fonctionne dès l'installation (ENF2)
        mockMvc.perform(get("/api/etudiants").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nom").value("Alice K."));
    }

    @Test
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
