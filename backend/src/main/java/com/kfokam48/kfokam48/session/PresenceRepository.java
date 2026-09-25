package com.kfokam48.kfokam48.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceRepository extends JpaRepository<PresenceEntity, Long> {

    /** Support du 409 DEJA_PRESENT (EF3) — aussi garanti par l'unicité en base. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /** EF5/RG13 : liste des présents d'une session, nom de l'étudiant chargé en une requête. */
    @Query("select p from PresenceEntity p join fetch p.etudiant "
            + "where p.session.id = :sessionId order by p.marqueeAt, p.id")
    List<PresenceEntity> listerParSession(@Param("sessionId") Long sessionId);
}
