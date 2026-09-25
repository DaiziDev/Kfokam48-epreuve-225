import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { ApiError } from './erreur-api';

/**
 * Normalise TOUTES les erreurs HTTP en ApiError { code, message } :
 * - corps du backend au format du contrat → on le garde tel quel ;
 * - réseau injoignable, backend down, corps illisible → codes génériques.
 * Les composants ne voient donc qu'un seul type d'erreur, toujours affichable.
 */
export const intercepteurErreurs: HttpInterceptorFn = (requete, suivant) =>
  suivant(requete).pipe(
    catchError((erreur: unknown) => {
      if (erreur instanceof HttpErrorResponse) {
        return throwError(() => versApiError(erreur));
      }
      return throwError(() => erreur);
    }),
  );

function versApiError(erreur: HttpErrorResponse): ApiError {
  const corps = erreur.error;
  if (corps && typeof corps === 'object' && typeof (corps as Record<string, unknown>)['code'] === 'string') {
    const { code, message } = corps as { code: string; message?: string };
    return new ApiError(code, message ?? 'Erreur sans message du serveur.', erreur.status);
  }
  if (erreur.status === 0) {
    return new ApiError('RESEAU_INJOIGNABLE', "Impossible de joindre le serveur. Vérifiez votre connexion.", 0);
  }
  return new ApiError('ERREUR_INTERNE', 'Une erreur inattendue est survenue.', erreur.status);
}
