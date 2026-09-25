import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiError } from '../../core/api/erreur-api';
import { IdentiteService } from '../../core/identite/identite.service';
import { RelecturesApiService } from '../../core/relectures/relectures-api.service';
import { RelectureAssignee } from '../../core/types/relecture';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { SelecteurIdentite } from '../partage/selecteur-identite';

@Component({
  selector: 'app-relecteur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe, ErreurApiPipe, UiIcon, SelecteurIdentite],
  templateUrl: './relecteur.html',
})
export class Relecteur {
  private readonly api = inject(RelecturesApiService);
  protected readonly identite = inject(IdentiteService);

  readonly relectures = signal<RelectureAssignee[]>([]);
  readonly chargement = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal<ApiError | null>(null);
  readonly succes = signal(false);
  readonly relectureActiveId = signal<number | null>(null);
  readonly relectureActive = computed(
    () => this.relectures().find((item) => item.id === this.relectureActiveId() && !item.rendue) ?? null,
  );
  readonly enAttente = computed(() => this.relectures().filter((item) => !item.rendue));
  readonly terminees = computed(() => this.relectures().filter((item) => item.rendue));

  note: number | null = null;
  commentaire = '';

  constructor() {
    effect(() => {
      const relecteurId = this.identite.etudiantId();
      this.succes.set(false);
      this.erreur.set(null);
      this.note = null;
      this.commentaire = '';
      if (relecteurId === null) {
        this.relectures.set([]);
        this.relectureActiveId.set(null);
        this.chargement.set(false);
      } else {
        this.charger(relecteurId);
      }
    });
  }

  charger(relecteurId = this.identite.etudiantId()): void {
    if (relecteurId === null) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);
    this.api.lister(relecteurId).subscribe({
      next: (relectures) => {
        if (this.identite.etudiantId() !== relecteurId) {
          return;
        }
        this.relectures.set(relectures);
        const selection = relectures.find((item) => item.id === this.relectureActiveId() && !item.rendue)
          ?? relectures.find((item) => !item.rendue)
          ?? null;
        this.relectureActiveId.set(selection?.id ?? null);
        this.chargement.set(false);
      },
      error: (erreur: ApiError) => {
        if (this.identite.etudiantId() === relecteurId) {
          this.erreur.set(erreur);
          this.chargement.set(false);
        }
      },
    });
  }

  selectionner(relecture: RelectureAssignee): void {
    if (relecture.rendue) {
      return;
    }
    this.relectureActiveId.set(relecture.id);
    this.note = null;
    this.commentaire = '';
    this.erreur.set(null);
    this.succes.set(false);
  }

  surNoteChange(valeur: string | number | null): void {
    if (valeur === null || (typeof valeur === 'string' && !valeur.trim())) {
      this.note = null;
      return;
    }
    const note = typeof valeur === 'number' ? valeur : Number(valeur);
    this.note = Number.isInteger(note) ? note : null;
  }

  rendre(): void {
    const relecteurId = this.identite.etudiantId();
    const relecture = this.relectureActive();
    const commentaire = this.commentaire.trim();
    if (
      relecteurId === null ||
      relecture === null ||
      this.note === null ||
      this.note < 0 ||
      this.note > 20 ||
      !commentaire ||
      commentaire.length > 4000 ||
      this.envoi()
    ) {
      return;
    }

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(false);
    this.api.rendre(relecture.id, { relecteurId, note: this.note, commentaire }).subscribe({
      next: () => {
        this.envoi.set(false);
        this.succes.set(true);
        this.charger(relecteurId);
      },
      error: (erreur: ApiError) => {
        this.envoi.set(false);
        this.erreur.set(erreur);
      },
    });
  }
}
