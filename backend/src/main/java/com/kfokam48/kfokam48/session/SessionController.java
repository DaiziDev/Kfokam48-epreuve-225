package com.kfokam48.kfokam48.session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
