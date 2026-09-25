import { Pipe, PipeTransform, inject } from '@angular/core';

import { ApiError } from '../../core/api/erreur-api';
import { I18nService } from './i18n.service';

/**
 * Affiche une ApiError dans la langue courante : {{ erreur() | erreurApi }}.
 * Impur pour la même raison que TPipe : suivre le changement de langue.
 */
@Pipe({ name: 'erreurApi', pure: false })
export class ErreurApiPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(erreur: ApiError | null | undefined): string {
    this.i18n.locale();
    if (!erreur) {
      return '';
    }
    return this.i18n.traduireErreur(erreur.code, erreur.message);
  }
}
