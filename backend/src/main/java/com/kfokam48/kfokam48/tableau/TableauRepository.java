package com.kfokam48.kfokam48.tableau;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.kfokam48.kfokam48.session.EtudiantEntity;

import jakarta.persistence.EntityManager;

/**
 * EF16 : agrégats du tableau de suivi, une requête GROUP BY par colonne — le
 * nombre de requêtes est fixe (5), quel que soit l'effectif de la promotion (ENF2).
 */
@Repository
public class TableauRepository {

    private final EntityManager entityManager;

    public TableauRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<EtudiantEntity> etudiantsDeLaPromotion(Long promotionId) {
        return entityManager.createQuery(
                "select e from EtudiantEntity e where e.promotion.id = :promotionId order by e.nom, e.id",
                EtudiantEntity.class)
                .setParameter("promotionId", promotionId)
                .getResultList();
    }

    /** Présences par étudiant, toutes sources confondues (ETUDIANT et FORMATEUR). */
    public Map<Long, Long> presencesParEtudiant(Long promotionId) {
        return compter("select p.etudiant.id, count(p) from PresenceEntity p "
                + "where p.etudiant.promotion.id = :promotionId group by p.etudiant.id", promotionId);
    }

    public Map<Long, Long> exercicesParAuteur(Long promotionId) {
        return compter("select e.auteur.id, count(e) from ExerciceEntity e "
                + "where e.auteur.promotion.id = :promotionId group by e.auteur.id", promotionId);
    }

    /** Relectures assignées à l'étudiant et pas encore rendues (définition tranchée en section 7). */
    public Map<Long, Long> relecturesEnAttenteParRelecteur(Long promotionId) {
        return compter("select r.relecteur.id, count(r) from RelectureEntity r "
                + "where r.relecteur.promotion.id = :promotionId and r.rendueAt is null "
                + "group by r.relecteur.id", promotionId);
    }

    /**
     * Notes regroupées par exercice afin que deux relectures ne pèsent qu'une fois.
     */
    public Map<Long, List<NotesExercice>> notesRecuesParAuteur(Long promotionId) {
        List<Object[]> lignes = entityManager.createQuery(
                "select e.auteur.id, e.id, sum(r.note), count(r) from RelectureEntity r join r.exercice e "
                        + "where e.auteur.promotion.id = :promotionId and r.rendueAt is not null "
                        + "group by e.auteur.id, e.id",
                Object[].class)
                .setParameter("promotionId", promotionId)
                .getResultList();
        Map<Long, List<NotesExercice>> resultat = new HashMap<>();
        for (Object[] ligne : lignes) {
            resultat.computeIfAbsent((Long) ligne[0], ignored -> new java.util.ArrayList<>())
                    .add(new NotesExercice((Long) ligne[1], ((Number) ligne[2]).longValue(),
                            ((Number) ligne[3]).longValue()));
        }
        return resultat;
    }

    private Map<Long, Long> compter(String jpql, Long promotionId) {
        List<Object[]> lignes = entityManager.createQuery(jpql, Object[].class)
                .setParameter("promotionId", promotionId)
                .getResultList();
        Map<Long, Long> resultat = new HashMap<>();
        for (Object[] ligne : lignes) {
            resultat.put((Long) ligne[0], ((Number) ligne[1]).longValue());
        }
        return resultat;
    }

    public record NotesExercice(Long exerciceId, long somme, long nombre) {
    }
}
