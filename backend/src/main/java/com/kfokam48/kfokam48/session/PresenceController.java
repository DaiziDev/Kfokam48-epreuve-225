package com.kfokam48.kfokam48.session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
public class PresenceController {

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @PostMapping("/api/presences")
    public ResponseEntity<PresenceCreeeResponse> marquer(@Valid @RequestBody MarquerPresenceRequest requete) {
        PresenceService.PresenceCreee resultat = service.enregistrer(requete.code(), requete.etudiantId());
        PresenceCreeeResponse reponse = new PresenceCreeeResponse(resultat.id(), resultat.sessionId(),
                resultat.etudiantId(), resultat.source());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    /** Corps du contrat : { code, etudiantId }, les deux obligatoires. */
    public record MarquerPresenceRequest(
            @NotBlank(message = "Le code est obligatoire.") String code,
            @NotNull(message = "L'etudiantId est obligatoire.") Long etudiantId) {
    }

    /** Réponse 201 strictement conforme au contrat. */
    public record PresenceCreeeResponse(Long id, Long sessionId, Long etudiantId, PresenceSource source) {
    }
}
