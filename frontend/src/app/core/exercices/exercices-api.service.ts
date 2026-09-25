import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../api/api-url';
import { ExerciceDetail, ExerciceEtat, ExerciceResume } from '../types/exercice';

/** Service API du domaine exercices (EF6, EF7, EF12). */
@Injectable({ providedIn: 'root' })
export class ExercicesApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  /** POST /api/exercices — EF6 : dépôt ; le relecteur est tiré côté serveur (EF8). */
  deposer(commande: { sessionId: number; etudiantId: number; lien: string }): Observable<ExerciceEtat> {
    return this.http.post<ExerciceEtat>(`${this.apiUrl}/exercices`, commande);
  }

  /** PUT /api/exercices/{id} — EF7 : remplacement du lien par son auteur. */
  remplacerLien(exerciceId: number, commande: { etudiantId: number; lien: string }): Observable<ExerciceEtat> {
    return this.http.put<ExerciceEtat>(`${this.apiUrl}/exercices/${exerciceId}`, commande);
  }

  /** GET /api/exercices?etudiantId= — « mes exercices » (id, session, lien, statut). */
  lister(etudiantId: number): Observable<ExerciceResume[]> {
    return this.http.get<ExerciceResume[]>(`${this.apiUrl}/exercices`, { params: { etudiantId } });
  }

  /** GET /api/exercices/{id} — EF12 : note et commentaire, jamais le relecteur (RG7). */
  consulter(exerciceId: number): Observable<ExerciceDetail> {
    return this.http.get<ExerciceDetail>(`${this.apiUrl}/exercices/${exerciceId}`);
  }
}
