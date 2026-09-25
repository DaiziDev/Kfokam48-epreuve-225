/**
 * Types miroir du contrat d'API (api/contrat.yml) — domaine présences.
 */
export type PresenceSource = 'ETUDIANT' | 'FORMATEUR';

/** Réponse 201 de POST /api/presences. */
export interface PresenceCreee {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: PresenceSource;
}
