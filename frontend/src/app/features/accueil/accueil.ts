import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CompteurDirective } from '../../ui/reveal/compteur.directive';
import { RevealDirective } from '../../ui/reveal/reveal.directive';
import { TPipe } from '../../ui/i18n/t.pipe';
import { UiIcon } from '../../ui/icon/ui-icon';
import { LucideIconName } from '../../ui/icon/lucide';

interface Etape {
  icone: LucideIconName;
  cleTitre: string;
  cleTexte: string;
}

interface Garantie {
  icone: LucideIconName;
  cleTitre: string;
  cleTexte: string;
}

/**
 * Page d'accueil : vitrine animée du produit (scroll-reveal, compteurs,
 * éléments flottants), entièrement i18n FR/EN, responsive 375px d'abord.
 */
@Component({
  selector: 'app-accueil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe, UiIcon, RevealDirective, CompteurDirective],
  templateUrl: './accueil.html',
  styles: `
    .grille-garanties {
      animation-delay: calc(var(--index) * 60ms);
    }
  `,
})
export class Accueil {
  /** Caractères du code affiché dans le mockup du hero. */
  readonly caracteresCode = 'K7P2M9'.split('');

  readonly stats = [
    { valeur: 15, cleTexte: 'accueil.stat-1' },
    { valeur: 1, cleTexte: 'accueil.stat-2' },
    { valeur: 24, cleTexte: 'accueil.stat-3' },
    { valeur: 2, cleTexte: 'accueil.stat-4' },
  ];

  readonly etapes: Etape[] = [
    { icone: 'calendar-plus', cleTitre: 'accueil.etape-1-titre', cleTexte: 'accueil.etape-1-texte' },
    { icone: 'scan-line', cleTitre: 'accueil.etape-2-titre', cleTexte: 'accueil.etape-2-texte' },
    { icone: 'clipboard-check', cleTitre: 'accueil.etape-3-titre', cleTexte: 'accueil.etape-3-texte' },
  ];

  readonly garanties: Garantie[] = [
    { icone: 'shuffle', cleTitre: 'accueil.garantie-1-titre', cleTexte: 'accueil.garantie-1-texte' },
    { icone: 'eye-off', cleTitre: 'accueil.garantie-2-titre', cleTexte: 'accueil.garantie-2-texte' },
    { icone: 'lock', cleTitre: 'accueil.garantie-3-titre', cleTexte: 'accueil.garantie-3-texte' },
    { icone: 'clipboard-check', cleTitre: 'accueil.garantie-4-titre', cleTexte: 'accueil.garantie-4-texte' },
    { icone: 'bar-chart-3', cleTitre: 'accueil.garantie-5-titre', cleTexte: 'accueil.garantie-5-texte' },
    { icone: 'users', cleTitre: 'accueil.garantie-6-titre', cleTexte: 'accueil.garantie-6-texte' },
  ];
}
