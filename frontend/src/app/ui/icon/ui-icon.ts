import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LUCIDE_ICONS, LucideIconName } from './lucide';

/**
 * Icône vectorielle du design system — trait 2px Lucide, taille contrôlée par
 * la taille de police (width/height 1em). Zéro emoji, zéro bitmap : cohérence
 * de trait garantie sur tous les écrans.
 *
 * Usage : <ui-icon name="calendar-plus" class="text-base" />
 */
@Component({
  selector: 'ui-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      [innerHTML]="chemin()"
    ></svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      width: 1em;
      height: 1em;
      flex-shrink: 0;
    }

    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class UiIcon {
  /** Nom de l'icône dans le registre Lucide (voir lucide.ts). */
  readonly name = input.required<LucideIconName>();

  readonly chemin = computed(() => LUCIDE_ICONS[this.name()] ?? '');
}
