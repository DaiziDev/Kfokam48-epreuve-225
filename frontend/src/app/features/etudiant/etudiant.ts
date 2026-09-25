import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PresencesApiService } from '../../core/presences/presences-api.service';
import { ApiError } from '../../core/api/erreur-api';
import { TPipe } from '../../ui/i18n/t.pipe';
import { I18nService } from '../../ui/i18n/i18n.service';
import { UiIcon } from '../../ui/icon/ui-icon';
import { CodeInput } from '../../ui/code-input/code-input';

/**
 * Écran étudiant : saisie du code de présence (EF2/EF3).
 * Premier dialogue frontend ↔ backend de l'application.
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
        <label for="code-presence" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200">
          {{ 'etudiant.etiquette-code' | t }}
        </label>
        <ui-code-input
          ngModel
          name="code"
          (ngModelChange)="surCodeChange($event)"
          [etiquette]="'etudiant.etiquette-code' | t"
        />
      </div>

      <button type="button" class="btn-primary mt-6 w-full" [disabled]="chargement() || !codeComplet" (click)="valider()">
        @if (chargement()) {
          <span class="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white"></span>
        }
        {{ 'etudiant.action' | t }}
      </button>

      @if (erreur()) {
        <p class="mt-4 flex items-start gap-2 rounded-lg border border-danger-border bg-danger-soft p-4 text-sm text-danger" role="alert">
          <ui-icon name="alert" class="mt-0.5 text-base" />
          {{ erreur()!.message }}
        </p>
      }

      @if (succes()) {
        <p class="mt-4 flex items-start gap-2 rounded-lg border border-success-border bg-success-soft p-4 text-sm text-success" role="status">
          <ui-icon name="check-circle-2" class="mt-0.5 text-base" />
          {{ 'etudiant.succes' | t }}
        </p>
      }
    </section>
  `,
})
export class Etudiant {
  private readonly presences = inject(PresencesApiService);
  private readonly i18n = inject(I18nService);

  readonly chargement = signal(false);
  readonly erreur = signal<ApiError | null>(null);
  readonly succes = signal(false);

  /** Dernier code complet tapé — relu depuis l'événement du CodeInput. */
  codeComplet = false;
  private codeSaisi = '';

  surCodeChange(valeur: string): void {
    this.codeSaisi = valeur;
    this.codeComplet = valeur.length === 6;
  }

  valider(): void {
    if (!this.codeSaisi || this.chargement()) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);
    this.succes.set(false);

    // L'identité de l'étudiant est fixée pour la démo (auth hors périmètre, §3 du cahier des charges).
    this.presences.marquerPresence({ code: this.codeSaisi, etudiantId: 1 }).subscribe({
      next: () => {
        this.chargement.set(false);
        this.succes.set(true);
      },
      error: (erreur: ApiError) => {
        this.chargement.set(false);
        this.erreur.set(erreur);
      },
    });
  }
}
