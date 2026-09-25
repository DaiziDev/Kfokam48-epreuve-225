import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';

import { PROMOTION_COURANTE } from '../../core/api/promotion';
import { ApiError } from '../../core/api/erreur-api';
import { EtudiantsApiService } from '../../core/etudiants/etudiants-api.service';
import { IdentiteService } from '../../core/identite/identite.service';
import { Etudiant } from '../../core/types/etudiant';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';

/**
 * « Je suis : [Alice K. ▾] » — partagé par les écrans étudiant et relecteur,
 * pour jouer le parcours complet à plusieurs identités (auth hors périmètre, Q1).
 */
@Component({
  selector: 'app-selecteur-identite',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe, ErreurApiPipe, UiIcon],
  template: `
    <div class="flex flex-wrap items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-800">
      <ui-icon name="users" class="text-base text-slate-500 dark:text-slate-400" />
      <label for="identite" class="text-sm font-medium text-slate-700 dark:text-slate-200">
        {{ 'identite.je-suis' | t }}
      </label>
      <select
        id="identite"
        class="min-w-0 flex-1 rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-200 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
        [value]="identite.etudiantId() ?? ''"
        (change)="surChoix($any($event.target).value)"
      >
        <option value="">{{ 'identite.choisir' | t }}</option>
        @for (etudiant of etudiants(); track etudiant.id) {
          <option [value]="etudiant.id">{{ etudiant.nom }}</option>
        }
      </select>
    </div>
    @if (erreur()) {
      <p class="mt-2 text-sm text-danger" role="alert">{{ erreur() | erreurApi }}</p>
    }
  `,
})
export class SelecteurIdentite implements OnInit {
  private readonly api = inject(EtudiantsApiService);
  private readonly promotionId = inject(PROMOTION_COURANTE);
  protected readonly identite = inject(IdentiteService);

  readonly etudiants = signal<Etudiant[]>([]);
  readonly erreur = signal<ApiError | null>(null);

  ngOnInit(): void {
    this.api.lister(this.promotionId).subscribe({
      next: (etudiants) => {
        this.etudiants.set(etudiants);
        // Identité retenue d'une visite précédente mais disparue (base réinitialisée)
        const courant = this.identite.etudiantId();
        if (courant !== null && !etudiants.some((e) => e.id === courant)) {
          this.identite.choisir(null);
        }
      },
      error: (erreur: ApiError) => this.erreur.set(erreur),
    });
  }

  surChoix(valeur: string): void {
    const choisi = this.etudiants().find((e) => String(e.id) === valeur) ?? null;
    this.identite.choisir(choisi);
  }
}
