import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

/**
 * Pipe de traduction : {{ 'nav.etudiant' | t }}.
 *
 * IMPURE volontairement : un pipe pur est mémoïsé par Angular sur sa clé
 * d'entrée — même quand le signal de locale change, transform n'est plus
 * rappelé et la langue ne bascule jamais (bug constaté). Avec pure: false,
 * transform ré-exécute à chaque cycle ; la lecture du signal de locale
 * marque chaque composant OnPush comme à rafraîchir au bon moment.
 */
@Pipe({ name: 't', pure: false })
export class TPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(cle: string): string {
    this.i18n.locale();
    return this.i18n.traduire(cle);
  }
}
