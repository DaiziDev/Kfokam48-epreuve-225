package com.kfokam48.kfokam48.exercice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<ExerciceEntity, Long> {

    /** Support du 409 EXERCICE_DEJA_DEPOSE — aussi garanti par l'unicité en base. */
    boolean existsBySessionIdAndAuteurId(Long sessionId, Long auteurId);
}
