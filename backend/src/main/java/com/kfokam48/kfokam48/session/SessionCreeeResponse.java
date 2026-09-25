package com.kfokam48.kfokam48.session;

import java.time.Instant;

/**
 * Réponse 201 de POST /api/sessions, strictement conforme au contrat :
 * id, code, ouvertureAt, expirationAt, statut. Un DTO dédié — jamais l'entité
 * JPA (condition de RG7 à terme : le DTO est le rempart contre les fuites).
 */
public record SessionCreeeResponse(Long id, String code, Instant ouvertureAt,
        Instant expirationAt, SessionStatut statut) {
}
