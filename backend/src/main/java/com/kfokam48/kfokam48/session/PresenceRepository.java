package com.kfokam48.kfokam48.session;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<PresenceEntity, Long> {

    /** Support du 409 DEJA_PRESENT (EF3) — aussi garanti par l'unicité en base. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
