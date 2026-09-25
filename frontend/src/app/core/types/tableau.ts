/**
 * Type miroir du contrat d'API (api/contrat.yml) — tableau de suivi (EF16).
 * La moyenne vient de l'API, jamais recalculée ici (ENF3).
 */
export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  moyenneProvisoire: boolean;
  relecturesEnAttente: number;
}
