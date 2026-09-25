import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { PresenceCreee } from '../types/presence';

/**
 * Service API du domaine présences. Les composants appellent ces méthodes,
 * jamais HttpClient directement (§8 du cahier des charges).
 */
@Injectable({ providedIn: 'root' })
export class PresencesApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** POST /api/presences — EF2/EF3 : l'étudiant marque sa présence. */
  marquerPresence(commande: { code: string; etudiantId: number }): Observable<PresenceCreee> {
    return this.http.post<PresenceCreee>(`${this.apiUrl}/presences`, commande);
  }
}
