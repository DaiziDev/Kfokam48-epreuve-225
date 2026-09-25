import { Injectable, computed, signal } from '@angular/core';

export type Theme = 'clair' | 'sombre';

const CLE_STOCKAGE = 'kfokam48-theme';

function themeInitial(): Theme {
  try {
    const stocke = localStorage.getItem(CLE_STOCKAGE);
    if (stocke === 'clair' || stocke === 'sombre') {
      return stocke;
    }
  } catch {
    // Stockage indisponible (navigation privée) — on retombe sur l'OS
  }
  return typeof matchMedia !== 'undefined' && matchMedia('(prefers-color-scheme: dark)').matches
    ? 'sombre'
    : 'clair';
}

function appliquer(theme: Theme): void {
  document.documentElement.classList.toggle('dark', theme === 'sombre');
}

/**
 * Thème clair/sombre : classe .dark sur <html> (variante @custom-variant
 * Tailwind 4), persisté en localStorage, initialisé depuis la préférence OS.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly themeCourant = signal<Theme>(themeInitial());

  readonly theme = this.themeCourant.asReadonly();
  readonly estSombre = computed(() => this.themeCourant() === 'sombre');

  constructor() {
    appliquer(this.themeCourant());
  }

  basculer(): void {
    const nouveau: Theme = this.estSombre() ? 'clair' : 'sombre';
    this.themeCourant.set(nouveau);
    appliquer(nouveau);
    try {
      localStorage.setItem(CLE_STOCKAGE, nouveau);
    } catch {
      // Persistance impossible : le thème reste actif pour la session
    }
  }
}
