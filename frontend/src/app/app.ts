import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { I18nService } from './ui/i18n/i18n.service';
import { TPipe } from './ui/i18n/t.pipe';
import { ThemeService } from './ui/theme/theme.service';
import { UiIcon } from './ui/icon/ui-icon';

/**
 * Coquille de l'application : navigation par rôle + bascules FR/EN et clair/sombre.
 * Aucun métier ici — les écrans sont des features indépendantes.
 */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TPipe, UiIcon],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly i18n = inject(I18nService);
  private readonly theme = inject(ThemeService);

  readonly estFrancais = this.i18n.estFrancais;
  readonly estSombre = this.theme.estSombre;

  basculerLangue(): void {
    this.i18n.basculer();
  }

  basculerTheme(): void {
    this.theme.basculer();
  }
}
