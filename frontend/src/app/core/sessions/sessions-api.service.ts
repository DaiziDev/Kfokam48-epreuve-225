import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { SessionCreee } from '../types/session';

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
}
