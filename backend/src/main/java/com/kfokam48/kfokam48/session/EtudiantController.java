package com.kfokam48.kfokam48.session;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liste des étudiants d'une promotion. Sans authentification (Q1), c'est ce qui
 * permet au frontend de choisir « qui je suis » pour jouer le parcours complet
 * (Alice dépose, Boris relit) — décision section 7.
 */
@RestController
public class EtudiantController {

    private final EtudiantRepository etudiants;
    private final PromotionRepository promotions;

    public EtudiantController(EtudiantRepository etudiants, PromotionRepository promotions) {
        this.etudiants = etudiants;
        this.promotions = promotions;
    }

    @GetMapping("/api/etudiants")
    @Transactional(readOnly = true)
    public List<EtudiantResponse> lister(@RequestParam("promotionId") Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new PromotionInconnueException(promotionId);
        }
        return etudiants.findByPromotionIdOrderByNomAscIdAsc(promotionId).stream()
                .map(e -> new EtudiantResponse(e.getId(), e.getNom()))
                .toList();
    }

    public record EtudiantResponse(Long id, String nom) {
    }
}
