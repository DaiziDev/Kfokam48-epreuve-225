import { Injectable, computed, signal } from '@angular/core';

import { Etudiant } from '../types/etudiant';

const CLE_STOCKAGE = 'kfokam48-identite';

function identiteInitiale(): Etudiant | null {
  try {
    const brut = localStorage.getItem(CLE_STOCKAGE);
    if (brut) {
      const lu = JSON.parse(brut) as Partial<Etudiant>;
      if (typeof lu.id === 'number' && typeof lu.nom === 'string') {
        return { id: lu.id, nom: lu.nom };
      }
    }
  } catch {
    // Stockage indisponible ou valeur corrompue : aucune identité choisie
  }
  return null;
}

/**
 * « Qui suis-je ? » côté étudiant. L'authentification est hors périmètre (Q1) :
 * l'identité est choisie dans une liste, comme le contrat attend un etudiantId
 * déclaratif. Retenue entre deux visites, jamais transmise ailleurs qu'à l'API.
 */
@Injectable({ providedIn: 'root' })
export class IdentiteService {
  private readonly courante = signal<Etudiant | null>(identiteInitiale());

  readonly etudiant = this.courante.asReadonly();
  readonly etudiantId = computed(() => this.courante()?.id ?? null);

  choisir(etudiant: Etudiant | null): void {
    this.courante.set(etudiant);
    try {
      if (etudiant) {
        localStorage.setItem(CLE_STOCKAGE, JSON.stringify(etudiant));
      } else {
        localStorage.removeItem(CLE_STOCKAGE);
      }
    } catch {
      // Persistance impossible : l'identité reste active pour la visite
    }
  }
}
