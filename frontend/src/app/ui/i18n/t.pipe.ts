import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

/**
 * Pipe de traduction : {{ 'nav.etudiant' | t }}. Pure avec injection du
 * service : le signal interne du service force le recalcul à chaque bascule.
 */
@Pipe({ name: 't' })
export class TPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(cle: string): string {
    return this.i18n.traduire(cle);
  }
}
