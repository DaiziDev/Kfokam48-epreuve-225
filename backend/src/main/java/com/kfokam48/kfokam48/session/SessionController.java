package com.kfokam48.kfokam48.session;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessionCreeeResponse> ouvrir(@Valid @RequestBody OuvrirSessionRequest requete) {
        SessionService.SessionCreee resultat = service.ouvrirSession(requete.getTitre(), requete.getPromotionId());
        SessionCreeeResponse reponse = new SessionCreeeResponse(resultat.id(), resultat.code(),
                resultat.ouvertureAt(), resultat.expirationAt(), resultat.statut());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    /**
     * Sessions d'une promotion, la plus récente d'abord : le formateur retrouve
     * ses sessions après un rechargement, l'étudiant choisit où déposer (section 7).
     */
    @GetMapping
    public List<SessionResumeResponse> lister(@RequestParam("promotionId") Long promotionId) {
        return service.listerParPromotion(promotionId).stream()
                .map(s -> new SessionResumeResponse(s.id(), s.titre(), s.code(), s.ouvertureAt(), s.expirationAt(),
                        s.statut()))
                .toList();
    }

    /** EF13 : clôture manuelle d'une session OUVERTE. */
    @PatchMapping("/{id}/cloture")
    public StatutSessionResponse cloturer(@PathVariable("id") Long sessionId) {
        SessionService.SessionStatutChange resultat = service.cloturer(sessionId);
        return new StatutSessionResponse(resultat.id(), resultat.statut());
    }

    /** EF15 : réouverture d'une session clôturée par erreur, expirationAt inchangé. */
    @PatchMapping("/{id}/reouverture")
    public StatutSessionResponse rouvrir(@PathVariable("id") Long sessionId) {
        SessionService.SessionStatutChange resultat = service.rouvrir(sessionId);
        return new StatutSessionResponse(resultat.id(), resultat.statut());
    }

    /** Réponse 200 de clôture et réouverture, strictement conforme au contrat : { id, statut }. */
    public record StatutSessionResponse(Long id, SessionStatut statut) {
    }

    /** Ligne de la liste des sessions : le contenu de la réponse de création, plus le titre. */
    public record SessionResumeResponse(Long id, String titre, String code, Instant ouvertureAt,
            Instant expirationAt, SessionStatut statut) {
    }
}
