package com.kfokam48.kfokam48.tableau;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableauController {

    private final TableauService service;

    public TableauController(TableauService service) {
        this.service = service;
    }

    /** EF16 : récapitulatif par étudiant de la promotion. */
    @GetMapping("/api/tableau")
    public List<LigneTableauResponse> consulter(@RequestParam("promotionId") Long promotionId) {
        return service.construire(promotionId).stream()
                .map(l -> new LigneTableauResponse(l.etudiantId(), l.nom(), l.presences(), l.exercicesDeposes(),
                        l.moyenne(), l.moyenneProvisoire(), l.relecturesEnAttente()))
                .toList();
    }

    /** Ligne du contrat imposé : { etudiantId, nom, presences, exercicesDeposes, moyenne, relecturesEnAttente }. */
    public record LigneTableauResponse(Long etudiantId, String nom, int presences, int exercicesDeposes,
            BigDecimal moyenne, boolean moyenneProvisoire, int relecturesEnAttente) {
    }
}
