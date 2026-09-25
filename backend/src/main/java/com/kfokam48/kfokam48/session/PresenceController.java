package com.kfokam48.kfokam48.session;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** EF5/RG13 : le formateur ajoute la présence d'un étudiant qui n'a pas pu saisir le code. */
    @PostMapping("/api/sessions/{id}/presences")
    public ResponseEntity<PresenceCreeeResponse> ajouterManuellement(@PathVariable("id") Long sessionId,
            @Valid @RequestBody AjouterPresenceRequest requete) {
        PresenceService.PresenceCreee resultat = service.ajouterParFormateur(sessionId, requete.etudiantId());
        PresenceCreeeResponse reponse = new PresenceCreeeResponse(resultat.id(), resultat.sessionId(),
                resultat.etudiantId(), resultat.source());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    /** EF5 : les présents d'une session, la source distingue les ajouts du formateur. */
    @GetMapping("/api/sessions/{id}/presences")
    public List<PresenceListeeResponse> lister(@PathVariable("id") Long sessionId) {
        return service.listerParSession(sessionId).stream()
                .map(p -> new PresenceListeeResponse(p.id(), p.etudiantId(), p.nom(), p.source(), p.marqueeAt()))
                .toList();
    }

    /** Corps du contrat : { code, etudiantId }, les deux obligatoires. */
    public record MarquerPresenceRequest(
            @NotBlank(message = "Le code est obligatoire.") String code,
            @NotNull(message = "L'etudiantId est obligatoire.") Long etudiantId) {
    }

    /** Corps du contrat pour l'ajout manuel : { etudiantId } obligatoire. */
    public record AjouterPresenceRequest(
            @NotNull(message = "L'etudiantId est obligatoire.") Long etudiantId) {
    }

    /** Réponse 201 strictement conforme au contrat. */
    public record PresenceCreeeResponse(Long id, Long sessionId, Long etudiantId, PresenceSource source) {
    }

    /** Ligne de la liste des présents d'une session. */
    public record PresenceListeeResponse(Long id, Long etudiantId, String nom, PresenceSource source,
            Instant marqueeAt) {
    }
}
