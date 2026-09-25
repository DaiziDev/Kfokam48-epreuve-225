package com.kfokam48.kfokam48.relecture;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RelectureRepository extends JpaRepository<RelectureEntity, Long> {

    /** Toutes les relectures d'un exercice, de la première à la seconde. */
    List<RelectureEntity> findByExerciceIdOrderByRangAsc(Long exerciceId);

    default Optional<RelectureEntity> findByExerciceId(Long exerciceId) {
        return findByExerciceIdOrderByRangAsc(exerciceId).stream().findFirst();
    }

    long countByExerciceId(Long exerciceId);

    long countByExerciceIdAndRang(Long exerciceId, int rang);

    long countByExercice_SessionId(Long sessionId);

    long countByExerciceIdAndRendueAtIsNotNull(Long exerciceId);

    /** Le même relecteur ne relit qu'une fois le même exercice (uq_relecture_exercice_relecteur). */
    boolean existsByExerciceIdAndRelecteurId(Long exerciceId, Long relecteurId);

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
