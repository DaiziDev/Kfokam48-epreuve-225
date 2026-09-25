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
