import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { PresencesApiService } from '../../core/presences/presences-api.service';
import { ApiError } from '../../core/api/erreur-api';
import { PROMOTION_COURANTE } from '../../core/api/promotion';
import { ExercicesApiService } from '../../core/exercices/exercices-api.service';
import { IdentiteService } from '../../core/identite/identite.service';
import { SessionsApiService } from '../../core/sessions/sessions-api.service';
import { ExerciceResume } from '../../core/types/exercice';
import { SessionResume } from '../../core/types/session';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { CodeInput } from '../../ui/code-input/code-input';
import { SelecteurIdentite } from '../partage/selecteur-identite';

/**
 * Écran étudiant : saisie du code de présence (EF2/EF3).
 * Premier dialogue frontend ↔ backend de l'application.
 */
@Component({
  selector: 'app-etudiant',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe, ErreurApiPipe, UiIcon, CodeInput, SelecteurIdentite],
  templateUrl: './etudiant.html',
})
export class Etudiant {
  private readonly presences = inject(PresencesApiService);
  private readonly exercicesApi = inject(ExercicesApiService);
  private readonly sessionsApi = inject(SessionsApiService);
  private readonly promotionId = inject(PROMOTION_COURANTE);
  protected readonly identite = inject(IdentiteService);

  readonly chargement = signal(false);
  readonly erreur = signal<ApiError | null>(null);
  readonly succes = signal(false);
  readonly sessionsEligibles = signal<SessionResume[]>([]);
  readonly mesExercices = signal<ExerciceResume[]>([]);
  readonly chargementExercices = signal(false);
  readonly depotEnCours = signal(false);
  readonly erreurExercices = signal<ApiError | null>(null);
  readonly succesDepot = signal(false);
  readonly sessionExerciceId = signal<number | null>(null);
  readonly exerciceSelectionne = computed(() =>
    this.mesExercices().find((exercice) => exercice.sessionId === this.sessionExerciceId()) ?? null,
  );

  lienExercice = '';

  codeComplet = false;
  private codeSaisi = '';

  constructor() {
    effect(() => {
      const etudiantId = this.identite.etudiantId();
      if (etudiantId === null) {
        this.sessionsEligibles.set([]);
        this.mesExercices.set([]);
        this.sessionExerciceId.set(null);
        this.chargementExercices.set(false);
        this.erreurExercices.set(null);
      } else {
        this.chargerEspaceExercices(etudiantId);
      }
    });
  }

  surCodeChange(valeur: string): void {
    this.codeSaisi = valeur;
    this.codeComplet = valeur.length === 6;
  }

  valider(): void {
    const etudiantId = this.identite.etudiantId();
    if (!this.codeSaisi || etudiantId === null || this.chargement()) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);
    this.succes.set(false);

    // Identité choisie dans le sélecteur (auth hors périmètre, Q1).
    this.presences.marquerPresence({ code: this.codeSaisi, etudiantId }).subscribe({
      next: () => {
        this.chargement.set(false);
        this.succes.set(true);
        this.chargerEspaceExercices(etudiantId);
      },
      error: (erreur: ApiError) => {
        this.chargement.set(false);
        this.erreur.set(erreur);
      },
    });
  }

  chargerEspaceExercices(etudiantId = this.identite.etudiantId()): void {
    if (etudiantId === null) {
      return;
    }
    this.chargementExercices.set(true);
    this.erreurExercices.set(null);

    forkJoin({
      sessions: this.sessionsApi.lister(this.promotionId),
      exercices: this.exercicesApi.lister(etudiantId),
    }).subscribe({
      next: ({ sessions, exercices }) => {
        if (this.identite.etudiantId() !== etudiantId) {
          return;
        }
        this.mesExercices.set(exercices);
        const ouvertes = sessions.filter((session) => session.statut === 'OUVERTE');
        if (ouvertes.length === 0) {
          this.sessionsEligibles.set([]);
          this.sessionExerciceId.set(null);
          this.chargementExercices.set(false);
          return;
        }

        forkJoin(
          // Seules les sessions où cette identité a pointé sont proposées.
          // L'API vérifie aussi la présence et les règles d'éligibilité au dépôt.
          ouvertes.map((session) => this.sessionsApi.listerPresences(session.id)),
        ).subscribe({
          next: (listes) => {
            if (this.identite.etudiantId() !== etudiantId) {
              return;
            }
            const eligibles = ouvertes.filter((_, index) =>
              listes[index].some((presence) => presence.etudiantId === etudiantId),
            );
            this.sessionsEligibles.set(eligibles);
            const selectionCourante = this.sessionExerciceId();
            const sessionChoisie = eligibles.find((session) => session.id === selectionCourante)
              ?? eligibles[0]
              ?? null;
            this.sessionExerciceId.set(sessionChoisie?.id ?? null);
            this.lienExercice = this.lienPourSession(sessionChoisie?.id ?? null, exercices);
            this.chargementExercices.set(false);
          },
          error: (erreur: ApiError) => {
            if (this.identite.etudiantId() === etudiantId) {
              this.erreurExercices.set(erreur);
              this.chargementExercices.set(false);
            }
          },
        });
      },
      error: (erreur: ApiError) => {
        if (this.identite.etudiantId() === etudiantId) {
          this.erreurExercices.set(erreur);
          this.chargementExercices.set(false);
        }
      },
    });
  }

  surSessionExerciceChange(valeur: string): void {
    const sessionId = valeur ? Number(valeur) : null;
    this.sessionExerciceId.set(sessionId);
    this.lienExercice = this.lienPourSession(sessionId, this.mesExercices());
    this.erreurExercices.set(null);
    this.succesDepot.set(false);
  }

  deposerExercice(): void {
    const etudiantId = this.identite.etudiantId();
    const sessionId = this.sessionExerciceId();
    const lien = this.lienExercice.trim();
    const exercice = this.exerciceSelectionne();
    if (etudiantId === null || sessionId === null || !lien || this.depotEnCours()) {
      return;
    }
    if (exercice?.statut === 'RELU') {
      return;
    }

    this.depotEnCours.set(true);
    this.erreurExercices.set(null);
    this.succesDepot.set(false);
    const requete = exercice
      ? this.exercicesApi.remplacerLien(exercice.id, { etudiantId, lien })
      : this.exercicesApi.deposer({ sessionId, etudiantId, lien });
    requete.subscribe({
      next: () => {
        this.depotEnCours.set(false);
        this.succesDepot.set(true);
        this.chargerEspaceExercices(etudiantId);
      },
      error: (erreur: ApiError) => {
        this.depotEnCours.set(false);
        this.erreurExercices.set(erreur);
      },
    });
  }

  private lienPourSession(sessionId: number | null, exercices: ExerciceResume[]): string {
    return exercices.find((exercice) => exercice.sessionId === sessionId)?.lien ?? '';
  }
}
