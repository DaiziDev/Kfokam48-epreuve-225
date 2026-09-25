import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SessionsApiService } from '../../core/sessions/sessions-api.service';
import { ApiError } from '../../core/api/erreur-api';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { I18nService } from '../../ui/i18n/i18n.service';
import { SessionCreee } from '../../core/types/session';

/**
 * Écran formateur : ouverture de session (EF1) avec affichage du code de
 * présence et compte à rebours jusqu'à l'expiration (RG1, 15 minutes).
 */
@Component({
  selector: 'app-formateur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe, UiIcon],
  templateUrl: './formateur.html',
})
export class Formateur {
  private readonly sessions = inject(SessionsApiService);
  private readonly i18n = inject(I18nService);

  readonly chargement = signal(false);
  readonly erreur = signal<ApiError | null>(null);
  readonly session = signal<SessionCreee | null>(null);

  titre = '';

  ouvrir(): void {
    if (!this.titre.trim() || this.chargement()) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);

    this.sessions
      .ouvrirSession({ titre: this.titre.trim(), promotionId: 1 })
      .subscribe({
        next: (session) => {
          this.session.set(session);
          this.chargement.set(false);
          this.demarrerCompteARebours();
        },
        error: (erreur: ApiError) => {
          this.erreur.set(erreur);
          this.chargement.set(false);
        },
      });
  }

  /** Compte à rebours jusqu'à expirationAt (RG1) — minute par minute. */
  private demarrerCompteARebours(): void {
    const session = this.session();
    if (!session) {
      return;
    }
    const expiration = new Date(session.expirationAt).getTime();
    const tick = () => {
      const restant = Math.max(0, expiration - Date.now());
      this.minutesRestantes.set(Math.floor(restant / 60000));
      if (restant > 0) {
        setTimeout(tick, 1000);
      }
    };
    tick();
  }

  readonly minutesRestantes = signal<number | null>(null);
  readonly expirationPassee = computed(() => (this.minutesRestantes() ?? 1) <= 0);
}
