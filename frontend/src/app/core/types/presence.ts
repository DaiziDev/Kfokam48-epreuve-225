/**
 * Types miroir du contrat d'API (api/contrat.yml) — domaine présences.
 */
export type PresenceSource = 'ETUDIANT' | 'FORMATEUR';

/** Réponse 201 de POST /api/presences et POST /api/sessions/{id}/presences. */
export interface PresenceCreee {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: PresenceSource;
}

/** Ligne de GET /api/sessions/{id}/presences (EF5) — la source distingue les ajouts du formateur. */
export interface PresenceListee {
  id: number;
  etudiantId: number;
  nom: string;
  source: PresenceSource;
  marqueeAt: string;
}
