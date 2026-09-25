import { InjectionToken } from '@angular/core';

/**
 * Promotion de travail. La gestion des promotions est hors périmètre (§3) :
 * l'application sert la promotion de démonstration (migration V2, id 1).
 * Un seul endroit à changer — jamais d'identifiant en dur dans un composant.
 */
export const PROMOTION_COURANTE = new InjectionToken<number>('PROMOTION_COURANTE', {
  factory: () => 1,
});
