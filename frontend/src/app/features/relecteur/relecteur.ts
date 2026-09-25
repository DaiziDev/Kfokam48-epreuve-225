import { Component } from '@angular/core';

@Component({
  selector: 'app-relecteur',
  imports: [],
  template: `
    <main class="mx-auto max-w-md px-4 py-8">
      <h1 class="text-2xl font-semibold text-slate-900">Mes relectures</h1>
      <p class="mt-2 text-slate-600">
        Les relectures qui m'ont été assignées, à rendre une par une.
      </p>
      <p class="mt-6 rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        Écran en attente des tickets EF9–EF12 (rendu de relecture, consultation de la note).
      </p>
    </main>
  `,
})
export class Relecteur {}
