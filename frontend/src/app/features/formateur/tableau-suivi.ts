import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { ApiError } from '../../core/api/erreur-api';
import { PROMOTION_COURANTE } from '../../core/api/promotion';
import { TableauApiService } from '../../core/tableau/tableau-api.service';
import { LigneTableau } from '../../core/types/tableau';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';

/**
 * EF16 : tableau de suivi. Cartes empilées sur mobile (aucun défilement
 * horizontal à 375px, ENF1), vrai tableau dès sm. La moyenne est affichée
 * telle que l'API la calcule (ENF3) — seul le format d'affichage est local.
 */
@Component({
  selector: 'app-tableau-suivi',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TPipe, ErreurApiPipe, UiIcon],
  template: `
    <section class="card p-5 sm:p-6">
      <div class="flex items-start justify-between gap-3">
        <div>
          <p class="kicker">{{ 'tableau.kicker' | t }}</p>
          <h2 class="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-50">{{ 'tableau.titre' | t }}</h2>
          <p class="tagline">{{ 'tableau.tagline' | t }}</p>
        </div>
        <button type="button" class="btn-secondary !px-2.5 !py-1.5 shrink-0" (click)="charger()" [attr.aria-label]="'commun.actualiser' | t">
          <ui-icon name="refresh-cw" class="text-sm" />
        </button>
      </div>

      @if (erreur()) {
        <p class="mt-4 flex items-start gap-2 rounded-lg border border-danger-border bg-danger-soft p-3 text-sm text-danger" role="alert">
          <ui-icon name="alert" class="mt-0.5 text-base" />
          {{ erreur() | erreurApi }}
        </p>
      } @else if (lignes().length === 0 && charge()) {
        <p class="note mt-4">{{ 'tableau.vide' | t }}</p>
      }

      <!-- Mobile : une carte par étudiant -->
      <ul class="mt-4 space-y-3 sm:hidden" data-test="tableau-cartes">
        @for (ligne of lignes(); track ligne.etudiantId) {
          <li class="rounded-lg border border-slate-200 p-3 dark:border-slate-800">
            <p class="break-words font-medium text-slate-900 dark:text-slate-100">{{ ligne.nom }}</p>
            <dl class="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 text-sm">
              <dt class="text-slate-500 dark:text-slate-400">{{ 'tableau.presences' | t }}</dt>
              <dd class="text-right font-medium">{{ ligne.presences }}</dd>
              <dt class="text-slate-500 dark:text-slate-400">{{ 'tableau.exercices' | t }}</dt>
              <dd class="text-right font-medium">{{ ligne.exercicesDeposes }}</dd>
              <dt class="text-slate-500 dark:text-slate-400">{{ 'tableau.moyenne' | t }}</dt>
              <dd class="text-right font-medium">
                @if (ligne.moyenne === null) {
                  <span class="text-slate-400">—</span>
                } @else {
                  {{ ligne.moyenne | number: '1.0-2' }}<span class="text-slate-400">/20</span>
                }
              </dd>
              <dt class="text-slate-500 dark:text-slate-400">{{ 'tableau.en-attente' | t }}</dt>
              <dd class="text-right font-medium" [class.text-warning]="ligne.relecturesEnAttente > 0">{{ ligne.relecturesEnAttente }}</dd>
            </dl>
          </li>
        }
      </ul>

      <!-- sm et plus : tableau -->
      @if (lignes().length > 0) {
        <div class="mt-4 hidden overflow-x-auto sm:block">
          <table class="w-full text-left text-sm" data-test="tableau">
            <thead class="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
              <tr>
                <th scope="col" class="py-2 pr-3 font-semibold">{{ 'tableau.etudiant' | t }}</th>
                <th scope="col" class="px-3 py-2 text-right font-semibold">{{ 'tableau.presences' | t }}</th>
                <th scope="col" class="px-3 py-2 text-right font-semibold">{{ 'tableau.exercices' | t }}</th>
                <th scope="col" class="px-3 py-2 text-right font-semibold">{{ 'tableau.moyenne' | t }}</th>
                <th scope="col" class="py-2 pl-3 text-right font-semibold">{{ 'tableau.en-attente' | t }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
              @for (ligne of lignes(); track ligne.etudiantId) {
                <tr>
                  <th scope="row" class="py-2.5 pr-3 font-medium text-slate-900 dark:text-slate-100">{{ ligne.nom }}</th>
                  <td class="px-3 py-2.5 text-right tabular-nums">{{ ligne.presences }}</td>
                  <td class="px-3 py-2.5 text-right tabular-nums">{{ ligne.exercicesDeposes }}</td>
                  <td class="px-3 py-2.5 text-right tabular-nums">
                    @if (ligne.moyenne === null) {
                      <span class="text-slate-400" [attr.aria-label]="'tableau.sans-note' | t">—</span>
                    } @else {
                      {{ ligne.moyenne | number: '1.0-2' }}<span class="text-slate-400">/20</span>
                    }
                  </td>
                  <td class="py-2.5 pl-3 text-right tabular-nums" [class.text-warning]="ligne.relecturesEnAttente > 0" [class.font-semibold]="ligne.relecturesEnAttente > 0">
                    {{ ligne.relecturesEnAttente }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class TableauSuivi {
  private readonly api = inject(TableauApiService);
  private readonly promotionId = inject(PROMOTION_COURANTE);

  readonly lignes = signal<LigneTableau[]>([]);
  readonly erreur = signal<ApiError | null>(null);
  readonly charge = signal(false);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.erreur.set(null);
    this.api.consulter(this.promotionId).subscribe({
      next: (lignes) => {
        this.lignes.set(lignes);
        this.charge.set(true);
      },
      error: (erreur: ApiError) => this.erreur.set(erreur),
    });
  }
}
