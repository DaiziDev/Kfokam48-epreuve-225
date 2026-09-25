import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';

/**
 * Écran relecteur — la liste des relectures assignées (EF9, GET /api/relectures)
 * et le formulaire de rendu (EF9, note 0-20 + commentaire) s'y brancheront.
 */
@Component({
  selector: 'app-relecteur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe, UiIcon],
  template: `
    <section class="card p-6">
      <p class="kicker">{{ 'relecteur.kicker' | t }}</p>
      <h1 class="screen-title">{{ 'relecteur.titre' | t }}</h1>
      <p class="tagline">{{ 'relecteur.tagline' | t }}</p>

      <p class="note mt-6">
        <ui-icon name="alert" class="mt-0.5 text-base" />
        {{ 'relecteur.en-attente' | t }}
      </p>
    </section>
  `,
})
export class Relecteur {}
