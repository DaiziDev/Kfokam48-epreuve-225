import { Directive, ElementRef, OnDestroy, OnInit, inject, input, numberAttribute } from '@angular/core';

/**
 * Compteur animé : quand l'élément entre dans le viewport, la valeur affichée
 * monte de 0 à la cible avec une décélération (easeOutCubic, requestAnimationFrame).
 * Usage : <span appCompteur="15">0</span>
 */
@Directive({ selector: '[appCompteur]' })
export class CompteurDirective implements OnInit, OnDestroy {
  /** Valeur cible du compteur. */
  readonly appCompteur = input.required({ transform: numberAttribute });

  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private observateur?: IntersectionObserver;
  private frame = 0;

  ngOnInit(): void {
    const cible = this.element.nativeElement;

    if (typeof IntersectionObserver === 'undefined') {
      cible.textContent = String(this.appCompteur());
      return;
    }

    this.observateur = new IntersectionObserver(
      (entrees) => {
        for (const entree of entrees) {
          if (entree.isIntersecting) {
            this.animer(cible);
            this.observateur?.disconnect();
          }
        }
      },
      { threshold: 0.4 },
    );
    this.observateur.observe(cible);
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.frame);
    this.observateur?.disconnect();
  }

  private animer(cible: HTMLElement): void {
    const valeurCible = this.appCompteur();
    const duree = 1400;
    const depart = performance.now();

    const pas = (maintenant: number) => {
      const progres = Math.min(1, (maintenant - depart) / duree);
      const amorti = 1 - Math.pow(1 - progres, 3); // easeOutCubic
      cible.textContent = String(Math.round(valeurCible * amorti));
      if (progres < 1) {
        this.frame = requestAnimationFrame(pas);
      }
    };
    this.frame = requestAnimationFrame(pas);
  }
}
