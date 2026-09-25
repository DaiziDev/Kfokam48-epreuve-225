import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

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

  private readonly sanitizer = inject(DomSanitizer);

  /**
   * bypassSecurityTrustHtml justifié : le HTML vient EXCLUSIVEMENT de notre
   * registre statique LUCIDE_ICONS (jamais d'une entrée utilisateur), et le
   * sanitizer par défaut strippait les éléments SVG (path, circle…) — les
   * icônes sortaient incomplètes (warnings de sanitization constatés aux tests).
   */
  readonly chemin = computed<SafeHtml>(
    () => this.sanitizer.bypassSecurityTrustHtml(LUCIDE_ICONS[this.name()] ?? ''),
  );
}
