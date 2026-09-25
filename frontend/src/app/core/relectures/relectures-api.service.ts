import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { RelectureAssignee, RelectureRendue } from '../types/relecture';

/** Service API du domaine relectures (EF9, EF10, EF11). */
@Injectable({ providedIn: 'root' })
export class RelecturesApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** GET /api/relectures?relecteurId= — EF9 : relectures à faire ou rendues. */
  lister(relecteurId: number): Observable<RelectureAssignee[]> {
    return this.http.get<RelectureAssignee[]>(`${this.apiUrl}/relectures`, {
      params: { relecteurId },
    });
  }

  /**
   * POST /api/relectures/{id} — EF9 : note 0–20 et commentaire. La validation
   * de la note (entière, bornes) reste côté API : aucun recalcul métier ici (§8).
   */
  rendre(
    relectureId: number,
    commande: { relecteurId: number; note: number; commentaire: string },
  ): Observable<RelectureRendue> {
    return this.http.post<RelectureRendue>(`${this.apiUrl}/relectures/${relectureId}`, commande);
  }
}
