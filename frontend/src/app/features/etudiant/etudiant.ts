import { Component } from '@angular/core';

@Component({
  selector: 'app-etudiant',
  imports: [],
  template: `
    <main class="mx-auto max-w-md px-4 py-8">
      <h1 class="text-2xl font-semibold text-slate-900">Espace étudiant</h1>
      <p class="mt-2 text-slate-600">
        Saisir le code de présence, déposer son exercice, faire sa relecture.
      </p>
      <p class="mt-6 rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        Écran en attente des tickets EF2+ (saisie du code, dépôt, relecture).
      </p>
    </main>
  `,
})
export class Etudiant {}
