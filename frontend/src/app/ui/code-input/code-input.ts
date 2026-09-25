import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  forwardRef,
  input,
  signal,
  viewChildren,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Saisie de code à 6 cases — la pièce maîtresse de l'écran étudiant.
 * Fonctionnalités : filtrage alphanumérique, passage auto à la case suivante,
 * Backspace intelligent (recule si la case est vide), collage réparti,
 * navigation flèches, accessible (labels + role="group").
 * Implémente ControlValueAccessor : utilisable avec [ngModel] ou reactive forms.
 */
@Component({
  selector: 'ui-code-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CodeInput),
      multi: true,
    },
  ],
  template: `
    <div role="group" [attr.aria-label]="etiquette()" class="flex gap-2">
      @for (valeur of cases(); track $index) {
        <input
          #caseInput
          type="text"
          inputmode="latin"
          autocomplete="one-time-code"
          maxlength="6"
          aria-label="Caractère {{ $index + 1 }} du code"
          class="h-14 w-11 rounded-lg border border-slate-300 bg-white text-center font-mono
                 text-xl font-semibold uppercase text-slate-900 shadow-card transition-colors
                 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-200
                 disabled:bg-slate-100 disabled:text-slate-400"
          [value]="valeur"
          (input)="surFrappe($index, $event)"
          (keydown)="surTouche($index, $event)"
          (paste)="surColler($event)"
          (focus)="toutSelectionner($event)"
        />
      }
    </div>
  `,
})
export class CodeInput implements ControlValueAccessor {
  private readonly entrees = viewChildren<ElementRef<HTMLInputElement>>('caseInput');

  readonly LONGUEUR = 6;
  readonly etiquette = input<string>('Code');

  private readonly valeurs = signal<string[]>(Array(this.LONGUEUR).fill(''));

  readonly cases = this.valeurs.asReadonly();

  readonly complet = computed(() => this.valeurs().every((c) => c.length === 1));

  private onChange?: (valeur: string) => void;
  private onTouched?: () => void;

  surFrappe(index: number, evenement: Event) {
    const entree = evenement.target as HTMLInputElement;
    const brut = entree.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();

    if (brut.length > 1) {
      this.remplirDepuis(index, brut);
      return;
    }
    this.poser(index, brut);
    if (brut !== '') {
      this.focaliser(index + 1);
    }
  }

  surTouche(index: number, evenement: KeyboardEvent) {
    if (evenement.key === 'Backspace' && this.lireCase(index) === '') {
      this.focaliser(index - 1);
    }
    if (evenement.key === 'ArrowLeft') {
      this.focaliser(index - 1);
    }
    if (evenement.key === 'ArrowRight') {
      this.focaliser(index + 1);
    }
  }

  surColler(evenement: ClipboardEvent) {
    evenement.preventDefault();
    const texte = evenement.clipboardData?.getData('text') ?? '';
    const propre = texte.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
    if (propre) {
      this.remplirDepuis(0, propre);
    }
  }

  toutSelectionner(evenement: FocusEvent) {
    (evenement.target as HTMLInputElement).select();
  }

  // --- ControlValueAccessor -------------------------------------------------

  writeValue(valeur: string | null): void {
    const caracteres = (valeur ?? '').toUpperCase().slice(0, this.LONGUEUR).split('');
    this.valeurs.set(Array.from({ length: this.LONGUEUR }, (_, i) => caracteres[i] ?? ''));
  }

  registerOnChange(fn: (valeur: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(estDesactive: boolean): void {
    this.desactive = estDesactive;
  }

  private desactive = false;

  // --- Internes -------------------------------------------------------------

  private poser(index: number, caractere: string) {
    this.valeurs.update((anciennes) => anciennes.with(index, caractere));
    this.notifier();
  }

  private remplirDepuis(index: number, texte: string) {
    this.valeurs.update((anciennes) => {
      const nouvelles = [...anciennes];
      let curseur = index;
      for (const caractere of texte) {
        if (curseur >= this.LONGUEUR) {
          break;
        }
        nouvelles[curseur] = caractere;
        curseur++;
      }
      return nouvelles;
    });
    this.focaliser(Math.min(index + texte.length, this.LONGUEUR - 1));
    this.notifier();
  }

  private lireCase(index: number): string {
    return this.valeurs()[index] ?? '';
  }

  private focaliser(index: number) {
    if (index < 0 || index >= this.LONGUEUR) {
      return;
    }
    this.entrees()[index]?.nativeElement.focus();
  }

  private notifier() {
    this.onChange?.(this.valeurs().join(''));
    this.onTouched?.();
  }
}
