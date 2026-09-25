package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF2/EF3 et EF5 : trajet HTTP complet sur H2 + Flyway, avec une horloge de test
 * déplaçable : T0 (création de session) puis T+20min (code expiré, 410).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PresenceControllerIntegrationTest.HorlogeDeTest.class)
class PresenceControllerIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-09-25T10:00:00Z");

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
    private HorlogeDeTest horlogeDeTest;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private EtudiantRepository etudiants;

    private Long promotionId;
    private Long etudiant1;
    private Long etudiant2;
    private Long etudiantAutrePromotion;

    @BeforeEach
    void preparer() {
        horlogeDeTest.horloge.avancerA(T0);

        PromotionEntity promotion = promotions.save(promotion("KFOKAM48"));
        promotionId = promotion.getId();

        etudiant1 = etudiants.save(etudiant(promotion, "Alice")).getId();
        etudiant2 = etudiants.save(etudiant(promotion, "Boris")).getId();

        PromotionEntity autrePromotion = promotions.save(promotion("KFOKAM49"));
        etudiantAutrePromotion = etudiants.save(etudiant(autrePromotion, "Chloé")).getId();
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

    /** Crée une session via l'API à l'heure courante de l'horloge de test. */
    private String creerSessionEtRetournerCode() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Algorithmique\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corps = resultat.getResponse().getContentAsString();
        int debut = corps.indexOf("\"code\":\"") + "\"code\":\"".length();
        return corps.substring(debut, corps.indexOf("\"", debut));
    }

    @Test
    void codeValideEnregistreLaPresenceAvecSourceEtudiant() throws Exception {
        String code = creerSessionEtRetournerCode();

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(etudiant1))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void codeAvecEspacesEtCasseDifferentesEstAccepte() throws Exception {
        String code = creerSessionEtRetournerCode();
        String codeSaisi = "  " + code.toLowerCase() + " ";

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + codeSaisi + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void codeInconnuRenvoie400CodeInconnu() throws Exception {
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void codeExpireRenvoie410CodeExpire() throws Exception {
        String code = creerSessionEtRetournerCode();

        // On déplace l'horloge 20 min après l'ouverture : la fenêtre de 15 min (RG1) est passée
        horlogeDeTest.horloge.avancerA(T0.plusSeconds(20 * 60));

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void deuxiemeTentativeRenvoie409DejaPresent() throws Exception {
        String code = creerSessionEtRetournerCode();
        String corps = "{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}";

        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void etudiantInconnuRenvoie404() throws Exception {
        String code = creerSessionEtRetournerCode();

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void sessionClotureeAvecCodeValideRenvoie409SessionCloturee() throws Exception {
        String code = creerSessionEtRetournerCode();

        // Le formateur clôture la session pendant que le code est encore valide
        // (état posé directement : ce test vise la présence, EF13 est testé dans SessionControllerIntegrationTest)
        SessionEntity session = sessionParCode(code);
        session.setStatut(SessionStatut.CLOTUREE);

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant2 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void codeExpirePrimeSurSessionCloturee() throws Exception {
        String code = creerSessionEtRetournerCode();
        SessionEntity session = sessionParCode(code);
        session.setStatut(SessionStatut.CLOTUREE);

        horlogeDeTest.horloge.avancerA(T0.plusSeconds(20 * 60));

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    // --- EF5/RG13 : présence ajoutée manuellement par le formateur ---

    private ResultActions ajouterManuellement(Long sessionId, Long etudiantId) throws Exception {
        return mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":" + etudiantId + "}"));
    }

    @Test
    void formateurAjouteUnePresenceAvecSourceFormateur() throws Exception {
        Long sessionId = sessionParCode(creerSessionEtRetournerCode()).getId();

        ajouterManuellement(sessionId, etudiant1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.etudiantId").value(etudiant1))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    void ajoutManuelResteSoupleApresExpirationDuCode() throws Exception {
        Long sessionId = sessionParCode(creerSessionEtRetournerCode()).getId();

        // RG1 ne vise que le code : 20 min après, le formateur peut encore rattraper (section 7)
        horlogeDeTest.horloge.avancerA(T0.plusSeconds(20 * 60));

        ajouterManuellement(sessionId, etudiant1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    void ajoutManuelSurSessionInconnueRenvoie404() throws Exception {
        ajouterManuellement(999999L, etudiant1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void ajoutManuelEtudiantInconnuRenvoie404() throws Exception {
        Long sessionId = sessionParCode(creerSessionEtRetournerCode()).getId();

        ajouterManuellement(sessionId, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void ajoutManuelEtudiantHorsPromotionRenvoie404() throws Exception {
        Long sessionId = sessionParCode(creerSessionEtRetournerCode()).getId();

        ajouterManuellement(sessionId, etudiantAutrePromotion)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void ajoutManuelSansEtudiantIdRenvoie400() throws Exception {
        Long sessionId = sessionParCode(creerSessionEtRetournerCode()).getId();

        mockMvc.perform(post("/api/sessions/" + sessionId + "/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMPS_REQUIS"));
    }

    @Test
    void ajoutManuelEtudiantDejaPresentParCodeRenvoie409() throws Exception {
        String code = creerSessionEtRetournerCode();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isCreated());

        ajouterManuellement(sessionParCode(code).getId(), etudiant1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void codeApresAjoutManuelRenvoie409DejaPresent() throws Exception {
        String code = creerSessionEtRetournerCode();
        ajouterManuellement(sessionParCode(code).getId(), etudiant1)
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void ajoutManuelSurSessionClotureeRenvoie409() throws Exception {
        SessionEntity session = sessionParCode(creerSessionEtRetournerCode());
        session.setStatut(SessionStatut.CLOTUREE);

        ajouterManuellement(session.getId(), etudiant1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void listeDesPresentsDistingueLesAjoutsDuFormateur() throws Exception {
        String code = creerSessionEtRetournerCode();
        Long sessionId = sessionParCode(code).getId();

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant1 + "}"))
                .andExpect(status().isCreated());
        horlogeDeTest.horloge.avancerA(T0.plusSeconds(60));
        ajouterManuellement(sessionId, etudiant2).andExpect(status().isCreated());

        mockMvc.perform(get("/api/sessions/" + sessionId + "/presences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].etudiantId").value(etudiant1))
                .andExpect(jsonPath("$[0].nom").value("Alice"))
                .andExpect(jsonPath("$[0].source").value("ETUDIANT"))
                .andExpect(jsonPath("$[1].etudiantId").value(etudiant2))
                .andExpect(jsonPath("$[1].nom").value("Boris"))
                .andExpect(jsonPath("$[1].source").value("FORMATEUR"))
                .andExpect(jsonPath("$[1].marqueeAt").isString());
    }

    @Test
    void listeDesPresentsSessionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/sessions/999999/presences"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Autowired
    private SessionRepository sessionRepository;

    private SessionEntity sessionParCode(String code) {
        return sessionRepository.findByCode(code).orElseThrow();
    }
}
