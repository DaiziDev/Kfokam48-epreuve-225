import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';

/**
 * Écran formateur — la création de session (EF1, via SessionsApiService) et le
 * tableau de suivi (EF16) s'y brancheront.
 */
@Component({
  selector: 'app-formateur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe, UiIcon],
  template: `
    <section class="card p-6">
      <p class="kicker">{{ 'formateur.kicker' | t }}</p>
      <h1 class="screen-title">{{ 'formateur.titre' | t }}</h1>
      <p class="tagline">{{ 'formateur.tagline' | t }}</p>

      <p class="note mt-6">
        <ui-icon name="alert" class="text-base" />
        {{ 'formateur.en-attente' | t }}
      </p>
    </section>
  `,
})
export class Formateur {}
