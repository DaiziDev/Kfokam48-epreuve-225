import { Component } from '@angular/core';

@Component({
  selector: 'app-formateur',
  imports: [],
  template: `
    <main class="mx-auto max-w-3xl px-4 py-8">
      <h1 class="text-2xl font-semibold text-slate-900">Espace formateur</h1>
      <p class="mt-2 text-slate-600">
        Ouvrir une session, partager le code, suivre la promotion.
      </p>
      <p class="mt-6 rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        Écran en attente des tickets EF1 (bouton ouvrir une session) et EF16 (tableau de suivi).
      </p>
    </main>
  `,
})
export class Formateur {}
