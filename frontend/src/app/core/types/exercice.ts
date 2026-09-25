/**
 * Types miroir du contrat d'API (api/contrat.yml) — domaine exercices.
 */
export type StatutExercice = 'EN_ATTENTE_RELECTURE' | 'RELU';

/** Réponse de POST /api/exercices (201) et PUT /api/exercices/{id} (200). */
export interface ExerciceEtat {
  id: number;
  statut: StatutExercice;
}

/** Ligne de GET /api/exercices?etudiantId= — « mes exercices », sans note ni relecteur. */
export interface ExerciceResume {
  id: number;
  sessionId: number;
  sessionTitre: string;
  lien: string;
  statut: StatutExercice;
}

/**
 * Réponse de GET /api/exercices/{id} (EF12). Aucun champ relecteur (RG7) ;
 * note et commentaire nuls tant que la relecture n'est pas rendue.
 */
export interface ExerciceDetail {
  id: number;
  lien: string;
  statut: StatutExercice;
  note: number | null;
  commentaire: string | null;
}
