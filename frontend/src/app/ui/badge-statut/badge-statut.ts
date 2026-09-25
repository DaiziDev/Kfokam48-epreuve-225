import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { UiIcon } from '../icon/ui-icon';
import { LucideIconName } from '../icon/lucide';

export type VarianteBadge = 'success' | 'warning' | 'danger' | 'neutral' | 'brand';

const CLASSES: Record<VarianteBadge, string> = {
  success: 'bg-success-soft text-success border-success-border',
  warning: 'bg-warning-soft text-warning border-warning-border',
  danger: 'bg-danger-soft text-danger border-danger-border',
  neutral: 'bg-slate-100 text-slate-600 border-slate-200',
  brand: 'bg-brand-50 text-brand-700 border-brand-200',
};

/**
 * Badge de statut : couleur sémantique + icône optionnelle. Sert aux statuts
 * d'exercice (EN_ATTENTE_RELECTURE / RELU) et aux états de session dans EF16.
 */
@Component({
  selector: 'ui-badge-statut',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiIcon],
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium"
      [class]="classes()"
    >
      @if (icone()) {
        <ui-icon [name]="icone()!" class="text-sm" />
      }
      {{ label() }}
    </span>
  `,
})
export class BadgeStatut {
  /** Texte affiché — déjà traduit par l'appelant. */
  readonly label = input.required<string>();
  readonly variante = input<VarianteBadge>('neutral');
  readonly icone = input<LucideIconName | undefined>(undefined);

  readonly classes = computed(() => CLASSES[this.variante()]);
}
