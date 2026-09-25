package com.kfokam48.kfokam48.exercice;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ExerciceRepository extends JpaRepository<ExerciceEntity, Long> {

    /** Support du 409 EXERCICE_DEJA_DEPOSE — aussi garanti par l'unicité en base. */
    boolean existsBySessionIdAndAuteurId(Long sessionId, Long auteurId);

    long countBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExerciceEntity e where e.id = :id")
    java.util.Optional<ExerciceEntity> verrouillerParId(@Param("id") Long id);

    /** « Mes exercices » : ceux de l'auteur, titre de session chargé en une requête. */
    @Query("select e from ExerciceEntity e join fetch e.session "
            + "where e.auteur.id = :auteurId order by e.deposeAt desc, e.id desc")
    List<ExerciceEntity> listerParAuteur(@Param("auteurId") Long auteurId);
}
