/**
 * Types miroir du contrat d'API (api/contrat.yml) — domaine relectures.
 */

/** Ligne de GET /api/relectures?relecteurId= (EF9). */
export interface RelectureAssignee {
  id: number;
  exerciceId: number;
  exerciceLien: string;
  rendue: boolean;
}

/** Réponse 200 de POST /api/relectures/{id} (EF9) — la note est désormais verrouillée (RG9). */
export interface RelectureRendue {
  id: number;
  exerciceId: number;
  note: number;
  commentaire: string;
  rendueAt: string;
}
