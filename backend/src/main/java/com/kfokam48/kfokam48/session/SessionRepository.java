package com.kfokam48.kfokam48.session;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    /** Utilisé par le retry de génération de code en cas de collision d'unicité. */
    boolean existsByCode(String code);
}
