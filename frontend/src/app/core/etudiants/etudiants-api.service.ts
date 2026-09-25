import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { Etudiant } from '../types/etudiant';

/** Service API des étudiants : alimente le sélecteur d'identité. */
@Injectable({ providedIn: 'root' })
export class EtudiantsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** GET /api/etudiants?promotionId= — étudiants de la promotion, triés par nom. */
  lister(promotionId: number): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`${this.apiUrl}/etudiants`, { params: { promotionId } });
  }
}
