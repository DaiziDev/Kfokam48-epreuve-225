import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { LigneTableau } from '../types/tableau';

/** Service API du tableau de suivi (EF16). */
@Injectable({ providedIn: 'root' })
export class TableauApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** GET /api/tableau?promotionId= — moyenne calculée côté API (ENF3). */
  consulter(promotionId: number): Observable<LigneTableau[]> {
    return this.http.get<LigneTableau[]>(`${this.apiUrl}/tableau`, { params: { promotionId } });
  }
}
