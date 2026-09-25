import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { Observable } from 'rxjs';

import { ApiError } from '../../core/api/erreur-api';
import { PROMOTION_COURANTE } from '../../core/api/promotion';
import { EtudiantsApiService } from '../../core/etudiants/etudiants-api.service';
import { SessionsApiService } from '../../core/sessions/sessions-api.service';
import { Etudiant } from '../../core/types/etudiant';
import { PresenceListee } from '../../core/types/presence';
import { SessionResume } from '../../core/types/session';
import { BadgeStatut } from '../../ui/badge-statut/badge-statut';
import { ErreurApiPipe } from '../../ui/i18n/erreur-api.pipe';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';

/**
 * Détail d'une session côté formateur : code et compte à rebours (RG1),
 * clôture / réouverture (EF13, EF15), présents avec leur source et ajout
 * manuel (EF5, RG13). Toutes les règles restent côté API : l'écran affiche.
 */
@Component({
  selector: 'app-session-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe, ErreurApiPipe, UiIcon, BadgeStatut],
  templateUrl: './session-detail.html',
})
export class SessionDetail {
  private readonly sessions = inject(SessionsApiService);
  private readonly etudiantsApi = inject(EtudiantsApiService);
  private readonly promotionId = inject(PROMOTION_COURANTE);

  readonly session = input.required<SessionResume>();
  /** Émis après clôture / réouverture : le parent recharge la liste. */
  readonly statutChange = output<void>();

  readonly presences = signal<PresenceListee[]>([]);
  readonly etudiants = signal<Etudiant[]>([]);
  readonly erreur = signal<ApiError | null>(null);
  readonly enCours = signal(false);
  readonly etudiantChoisi = signal<number | null>(null);

  /** Horloge d'affichage, rafraîchie chaque seconde et arrêtée en quittant l'écran. */
  private readonly maintenant = signal(Date.now());
  private presenceEnCours = false;

  readonly minutesRestantes = computed(() => {
    const restant = new Date(this.session().expirationAt).getTime() - this.maintenant();
    return restant > 0 ? Math.ceil(restant / 60000) : 0;
  });
  readonly codeExpire = computed(() => this.minutesRestantes() === 0);
  readonly estOuverte = computed(() => this.session().statut === 'OUVERTE');

  /** Étudiants pas encore présents : seuls candidats à l'ajout manuel. */
  readonly absents = computed(() => {
    const presents = new Set(this.presences().map((p) => p.etudiantId));
    return this.etudiants().filter((e) => !presents.has(e.id));
  });

  constructor() {
    const minuterie = setInterval(() => this.maintenant.set(Date.now()), 1000);
    // Les élèves pointent indépendamment : garder la liste formateur à jour sans action manuelle.
    const actualisationPresences = setInterval(() => {
      if (this.estOuverte()) {
        this.chargerPresences();
      }
    }, 5000);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(minuterie);
      clearInterval(actualisationPresences);
    });

    this.etudiantsApi.lister(this.promotionId).subscribe({
      next: (etudiants) => this.etudiants.set(etudiants),
      error: (erreur: ApiError) => this.erreur.set(erreur),
    });

    // Recharge les présents à chaque changement de session sélectionnée
    effect(() => {
      const id = this.session().id;
      this.erreur.set(null);
      this.etudiantChoisi.set(null);
      this.chargerPresences(id);
    });
  }

  chargerPresences(sessionId = this.session().id): void {
    if (this.presenceEnCours) {
      return;
    }
    this.presenceEnCours = true;
    this.sessions.listerPresences(sessionId).subscribe({
      next: (presences) => {
        this.presenceEnCours = false;
        if (sessionId === this.session().id) {
          this.presences.set(presences);
        }
      },
      error: (erreur: ApiError) => {
        this.presenceEnCours = false;
        this.erreur.set(erreur);
      },
    });
  }

  surChoixEtudiant(valeur: string): void {
    this.etudiantChoisi.set(valeur ? Number(valeur) : null);
  }

  ajouterPresence(): void {
    const etudiantId = this.etudiantChoisi();
    if (etudiantId === null || this.enCours()) {
      return;
    }
    this.executer(this.sessions.ajouterPresence(this.session().id, etudiantId), () => {
      this.etudiantChoisi.set(null);
      this.chargerPresences();
    });
  }

  basculerStatut(): void {
    const id = this.session().id;
    const appel = this.estOuverte() ? this.sessions.cloturer(id) : this.sessions.rouvrir(id);
    this.executer(appel, () => this.statutChange.emit());
  }

  private executer<T>(appel: Observable<T>, succes: () => void): void {
    if (this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    appel.subscribe({
      next: () => {
        this.enCours.set(false);
        succes();
      },
      error: (erreur: ApiError) => {
        this.enCours.set(false);
        this.erreur.set(erreur);
      },
    });
  }
}
