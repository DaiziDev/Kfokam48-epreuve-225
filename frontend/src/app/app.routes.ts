import { Routes } from '@angular/router';

/**
 * Lazy loading par écran : chaque feature charge son propre bundle.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/accueil/accueil').then((m) => m.Accueil),
  },
  {
    path: 'etudiant',
    loadComponent: () => import('./features/etudiant/etudiant').then((m) => m.Etudiant),
  },
  {
    path: 'relecteur',
    loadComponent: () => import('./features/relecteur/relecteur').then((m) => m.Relecteur),
  },
  {
    path: 'formateur',
    loadComponent: () => import('./features/formateur/formateur').then((m) => m.Formateur),
  },
  { path: '**', redirectTo: 'accueil' },
];
