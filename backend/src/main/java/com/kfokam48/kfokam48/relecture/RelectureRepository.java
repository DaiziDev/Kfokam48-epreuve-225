package com.kfokam48.kfokam48.relecture;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RelectureRepository extends JpaRepository<RelectureEntity, Long> {

    Optional<RelectureEntity> findByExerciceId(Long exerciceId);

    /** RG5 : un exercice a exactement un relecteur. */
    long countByExerciceId(Long exerciceId);

    /**
     * EF9/EF11 : verrou d'écriture sur la relecture le temps du rendu. Deux
     * soumissions simultanées sont sérialisées : la seconde voit la relecture
     * rendue et reçoit 409, la note n'est jamais écrasée (RG9).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RelectureEntity r where r.id = :id")
    Optional<RelectureEntity> verrouillerParId(@Param("id") Long id);

    /** EF9 : relectures d'un relecteur, lien de l'exercice chargé en une requête. */
    @Query("select r from RelectureEntity r join fetch r.exercice "
            + "where r.relecteur.id = :relecteurId order by r.id")
    List<RelectureEntity> listerParRelecteur(@Param("relecteurId") Long relecteurId);
}
