import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SessionsApiService } from '../../core/sessions/sessions-api.service';
import { ApiError } from '../../core/api/erreur-api';
import { PROMOTION_COURANTE } from '../../core/api/promotion';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { SessionResume } from '../../core/types/session';
import { SessionDetail } from './session-detail';
import { TableauSuivi } from './tableau-suivi';

/**
 * Écran formateur : ouverture de session (EF1) avec affichage du code de
 * présence et compte à rebours jusqu'à l'expiration (RG1, 15 minutes).
 */
@Component({
  selector: 'app-formateur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe, ErreurApiPipe, UiIcon, SessionDetail, TableauSuivi],
  templateUrl: './formateur.html',
})
export class Formateur {
  private readonly sessions = inject(SessionsApiService);
  private readonly promotionId = inject(PROMOTION_COURANTE);

  readonly chargement = signal(false);
  readonly erreur = signal<ApiError | null>(null);
  readonly sessionsListe = signal<SessionResume[]>([]);
  readonly session = signal<SessionResume | null>(null);
  readonly erreurListe = signal<ApiError | null>(null);

  titre = '';

  constructor() {
    this.chargerSessions();
  }

  chargerSessions(): void {
    this.erreurListe.set(null);
    this.sessions.lister(this.promotionId).subscribe({
      next: (liste) => {
        this.sessionsListe.set(liste);
        const selection = this.session();
        const sessionActualisee = selection && liste.find((element) => element.id === selection.id);
        if (sessionActualisee) {
          this.session.set(sessionActualisee);
        }
      },
      error: (erreur: ApiError) => this.erreurListe.set(erreur),
    });
  }

  ouvrir(): void {
    if (!this.titre.trim() || this.chargement()) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);

    this.sessions
      .ouvrirSession({ titre: this.titre.trim(), promotionId: this.promotionId })
      .subscribe({
        next: (session) => {
          this.session.set({ ...session, titre: this.titre.trim() });
          this.titre = '';
          this.chargement.set(false);
          this.chargerSessions();
        },
        error: (erreur: ApiError) => {
          this.erreur.set(erreur);
          this.chargement.set(false);
        },
      });
  }

  selectionnerSession(session: SessionResume): void {
    this.session.set(session);
  }

  reinitialiserSelection(): void {
    this.session.set(null);
    this.chargerSessions();
  }
}
