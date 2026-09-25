/**
 * Erreur normalisée au format imposé du contrat : { code, message }.
 * Toute erreur sortant de la couche API est une ApiError — les composants
 * n'ont jamais à connaître HttpErrorResponse.
 */
export class ApiError extends Error {
  constructor(
    /** Identifiant stable du contrat : CODE_EXPIRE, CHAMPS_REQUIS… */
    readonly code: string,
    message: string,
    /** Statut HTTP brut, pour les cas où l'écran doit réagir différemment. */
    readonly statutHttp: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function versErreurApi(erreur: unknown): ApiError {
  if (erreur instanceof ApiError) {
    return erreur;
  }
  return new ApiError('ERREUR_INTERNE', 'Une erreur inattendue est survenue.', 0);
}
