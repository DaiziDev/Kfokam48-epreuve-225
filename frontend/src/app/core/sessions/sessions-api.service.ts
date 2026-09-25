import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { PresenceCreee, PresenceListee } from '../types/presence';
import { SessionCreee, SessionResume, StatutSessionChange } from '../types/session';

/**
 * Un service par ressource du contrat. Les composants appellent uniquement
 * ces méthodes — jamais HttpClient, jamais d'URL écrite à la main.
 */
@Injectable({ providedIn: 'root' })
export class SessionsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** POST /api/sessions — EF1 : le formateur ouvre une session. */
  ouvrirSession(commande: { titre: string; promotionId: number }): Observable<SessionCreee> {
    return this.http.post<SessionCreee>(`${this.apiUrl}/sessions`, commande);
  }

  /** GET /api/sessions?promotionId= — sessions de la promotion, la plus récente d'abord. */
  lister(promotionId: number): Observable<SessionResume[]> {
    return this.http.get<SessionResume[]>(`${this.apiUrl}/sessions`, { params: { promotionId } });
  }

  /** PATCH /api/sessions/{id}/cloture — EF13. */
  cloturer(sessionId: number): Observable<StatutSessionChange> {
    return this.http.patch<StatutSessionChange>(`${this.apiUrl}/sessions/${sessionId}/cloture`, null);
  }

  /** PATCH /api/sessions/{id}/reouverture — EF15. */
  rouvrir(sessionId: number): Observable<StatutSessionChange> {
    return this.http.patch<StatutSessionChange>(`${this.apiUrl}/sessions/${sessionId}/reouverture`, null);
  }

  /** POST /api/sessions/{id}/presences — EF5 : présence ajoutée par le formateur. */
  ajouterPresence(sessionId: number, etudiantId: number): Observable<PresenceCreee> {
    return this.http.post<PresenceCreee>(`${this.apiUrl}/sessions/${sessionId}/presences`, { etudiantId });
  }

  /** GET /api/sessions/{id}/presences — EF5 : présents de la session, source incluse. */
  listerPresences(sessionId: number): Observable<PresenceListee[]> {
    return this.http.get<PresenceListee[]>(`${this.apiUrl}/sessions/${sessionId}/presences`);
  }
}
