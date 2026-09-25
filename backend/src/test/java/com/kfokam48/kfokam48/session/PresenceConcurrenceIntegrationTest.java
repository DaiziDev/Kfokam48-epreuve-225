package com.kfokam48.kfokam48.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * #22 — pointages simultanés. Vrai serveur HTTP et vraies requêtes concurrentes :
 * chaque scénario est rejoué plusieurs fois, la course étant intermittente.
 *
 * Pas de @Transactional : les requêtes s'exécutent dans leurs propres threads et
 * valident réellement leurs écritures. Le test nettoie donc lui-même ses données
 * (sa promotion dédiée) pour ne pas polluer les autres classes de test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PresenceConcurrenceIntegrationTest {

    private static final int TOURS = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private EtudiantRepository etudiants;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private ExecutorService executeur;
    private Long promotionId;
    private Long alice;
    private Long boris;

    @BeforeEach
    void preparer() {
        executeur = Executors.newFixedThreadPool(4);
        PromotionEntity promotion = new PromotionEntity();
        promotion.setNom("CONCURRENCE-" + System.nanoTime());
        promotionId = promotions.save(promotion).getId();
        alice = etudiants.save(etudiant(promotion, "Alice")).getId();
        boris = etudiants.save(etudiant(promotion, "Boris")).getId();
    }

    @AfterEach
    void nettoyer() throws InterruptedException {
        executeur.shutdownNow();
        executeur.awaitTermination(5, TimeUnit.SECONDS);
        String sessionsDeLaPromotion = "select id from session_cours where promotion_id = ?";
        jdbc.update("delete from presence where session_id in (" + sessionsDeLaPromotion + ")", promotionId);
        jdbc.update("delete from session_cours where promotion_id = ?", promotionId);
        jdbc.update("delete from etudiant where promotion_id = ?", promotionId);
        jdbc.update("delete from promotion where id = ?", promotionId);
    }

    private EtudiantEntity etudiant(PromotionEntity promotion, String nom) {
        EtudiantEntity e = new EtudiantEntity();
        e.setPromotion(promotion);
        e.setNom(nom);
        return e;
    }

    // --- Appels HTTP réels ---

    private HttpResponse<String> envoyer(String methode, String chemin, String corps) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Content-Type", "application/json");
        requete = corps == null ? requete.GET() : requete.method(methode, HttpRequest.BodyPublishers.ofString(corps));
        return http.send(requete.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode ouvrirSession() throws Exception {
        HttpResponse<String> reponse = envoyer("POST", "/api/sessions",
                "{\"titre\":\"Pointage concurrent\",\"promotionId\":" + promotionId + "}");
        assertThat(reponse.statusCode()).isEqualTo(201);
        return objectMapper.readTree(reponse.body());
    }

    /** Lance les pointages au même instant (latch) et renvoie les réponses. */
    private List<HttpResponse<String>> pointerEnMemeTemps(String code, List<Long> etudiantIds) throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> envois = new ArrayList<>();
        for (Long etudiantId : etudiantIds) {
            envois.add(executeur.submit(() -> {
                depart.await();
                return envoyer("POST", "/api/presences",
                        "{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}");
            }));
        }
        depart.countDown();
        List<HttpResponse<String>> reponses = new ArrayList<>();
        for (Future<HttpResponse<String>> envoi : envois) {
            reponses.add(envoi.get(10, TimeUnit.SECONDS));
        }
        return reponses;
    }

    private List<Long> presentsSelonLApi(long sessionId) throws Exception {
        HttpResponse<String> reponse = envoyer("GET", "/api/sessions/" + sessionId + "/presences", null);
        assertThat(reponse.statusCode()).isEqualTo(200);
        List<Long> ids = new ArrayList<>();
        objectMapper.readTree(reponse.body()).forEach(p -> ids.add(p.get("etudiantId").asLong()));
        return ids;
    }

    // --- Scénarios du ticket ---

    @Test
    void deuxEtudiantsQuiPointentEnMemeTempsSontTousLesDeuxEnregistres() throws Exception {
        for (int tour = 1; tour <= TOURS; tour++) {
            JsonNode session = ouvrirSession();

            List<HttpResponse<String>> reponses = pointerEnMemeTemps(session.get("code").asString(), List.of(alice, boris));

            assertThat(reponses).as("tour %d : les deux pointages sont acceptés", tour)
                    .allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(201));
            assertThat(presentsSelonLApi(session.get("id").asLong()))
                    .as("tour %d : l'API renvoie les deux présences", tour)
                    .containsExactlyInAnyOrder(alice, boris);
        }
    }

    @Test
    void unMemeEtudiantQuiPointeEnRafaleNeCreeQuUnePresenceEtJamaisDe500() throws Exception {
        for (int tour = 1; tour <= TOURS; tour++) {
            JsonNode session = ouvrirSession();

            List<HttpResponse<String>> reponses = pointerEnMemeTemps(session.get("code").asString(),
                    List.of(alice, alice, alice, alice));

            List<Integer> statuts = reponses.stream().map(HttpResponse::statusCode).toList();
            assertThat(statuts).as("tour %d : une création, trois refus DEJA_PRESENT (jamais 500)", tour)
                    .containsOnly(201, 409)
                    .containsOnlyOnce(201);
            reponses.stream().filter(r -> r.statusCode() == 409)
                    .forEach(r -> assertThat(r.body()).contains("DEJA_PRESENT"));
            assertThat(presentsSelonLApi(session.get("id").asLong()))
                    .as("tour %d : aucun doublon", tour)
                    .containsExactly(alice);
        }
    }
}
