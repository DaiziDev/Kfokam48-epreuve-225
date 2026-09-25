package com.kfokam48.kfokam48.tableau;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.session.PromotionInconnueException;
import com.kfokam48.kfokam48.session.PromotionRepository;

/**
 * EF16 : tableau de suivi par étudiant. Tous les chiffres, moyenne comprise,
 * sont calculés ici et jamais par le frontend (ENF3).
 */
@Service
public class TableauService {

    private final TableauRepository tableau;
    private final PromotionRepository promotions;

    public TableauService(TableauRepository tableau, PromotionRepository promotions) {
        this.tableau = tableau;
        this.promotions = promotions;
    }

    @Transactional(readOnly = true)
    public List<LigneTableau> construire(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new PromotionInconnueException(promotionId);
        }

        Map<Long, Long> presences = tableau.presencesParEtudiant(promotionId);
        Map<Long, Long> exercices = tableau.exercicesParAuteur(promotionId);
        Map<Long, TableauRepository.SommeNotes> notes = tableau.notesRecuesParAuteur(promotionId);
        Map<Long, Long> enAttente = tableau.relecturesEnAttenteParRelecteur(promotionId);

        // Un étudiant sans aucune activité figure quand même, avec des zéros
        return tableau.etudiantsDeLaPromotion(promotionId).stream()
                .map(e -> new LigneTableau(e.getId(), e.getNom(),
                        presences.getOrDefault(e.getId(), 0L).intValue(),
                        exercices.getOrDefault(e.getId(), 0L).intValue(),
                        moyenne(notes.get(e.getId())),
                        enAttente.getOrDefault(e.getId(), 0L).intValue()))
                .toList();
    }

    /**
     * Moyenne des notes reçues sur les relectures rendues, arrondie à 2
     * décimales (demi supérieur) ; nulle tant qu'aucune note n'est reçue (RG10).
     */
    static BigDecimal moyenne(TableauRepository.SommeNotes notes) {
        if (notes == null || notes.nombre() == 0) {
            return null;
        }
        return BigDecimal.valueOf(notes.somme())
                .divide(BigDecimal.valueOf(notes.nombre()), 2, RoundingMode.HALF_UP);
    }

    public record LigneTableau(Long etudiantId, String nom, int presences, int exercicesDeposes,
            BigDecimal moyenne, int relecturesEnAttente) {
    }
}
