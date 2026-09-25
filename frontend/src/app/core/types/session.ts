export type StatutSession = 'OUVERTE' | 'CLOTUREE';

/**
 * Types miroir du contrat d'API (api/contrat.yml) — source unique de vérité.
 * Toute évolution du contrat passe d'abord par ici, jamais par le composant.
 */
export interface SessionCreee {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
  statut: StatutSession;
}

/** Ligne de GET /api/sessions?promotionId= — la plus récente d'abord. */
export interface SessionResume extends SessionCreee {
  titre: string;
}

/** Réponse 200 de PATCH /api/sessions/{id}/cloture et /reouverture (EF13/EF15). */
export interface StatutSessionChange {
  id: number;
  statut: StatutSession;
}
