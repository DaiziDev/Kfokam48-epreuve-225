import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { CodeInput } from '../../ui/code-input/code-input';

/**
 * Écran étudiant — la saisie du code de présence (EF2/EF3) viendra s'y brancher
 * dès le premier dialogue frontend ↔ backend.
 */
@Component({
  selector: 'app-etudiant',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe, UiIcon, CodeInput],
  template: `
    <section class="card p-6">
      <p class="kicker">{{ 'etudiant.kicker' | t }}</p>
      <h1 class="screen-title">{{ 'etudiant.titre' | t }}</h1>
      <p class="tagline">{{ 'etudiant.tagline' | t }}</p>

      <div class="mt-6">
        <label for="code-presence" class="mb-2 block text-sm font-medium text-slate-700">
          {{ 'etudiant.etiquette-code' | t }}
        </label>
        <ui-code-input
          [ngModel]="code"
          (ngModelChange)="code = $event"
          name="code"
          [etiquette]="'etudiant.etiquette-code' | t"
        />
      </div>

      <button type="button" class="btn-primary mt-6 w-full" [disabled]="!code">
        <ui-icon name="check-circle-2" class="text-base" />
        {{ 'etudiant.action' | t }}
      </button>
      <p class="note mt-4">
        <ui-icon name="alert" class="mt-0.5 text-base" />
        {{ 'etudiant.en-attente' | t }}
      </p>
    </section>
  `,
})
export class Etudiant {
  code = '';
}
