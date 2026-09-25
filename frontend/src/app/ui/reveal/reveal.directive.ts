import { Directive, ElementRef, OnDestroy, OnInit, effect, inject, input, numberAttribute } from '@angular/core';

/**
 * Scroll-reveal : applique .reveal à l'élément, puis .reveal-visible quand il
 * entre dans le viewport (IntersectionObserver, déclenchement unique). Le délai
 * optionnel crée l'effet de cascade sur les grilles.
 * Usage : <div appReveal [appRevealDelay]="120">…</div>
 */
@Directive({ selector: '[appReveal]' })
export class RevealDirective implements OnInit, OnDestroy {
  /** Délai en ms avant l'apparition (cascade). */
  readonly appRevealDelay = input(0, { transform: numberAttribute });

  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private observateur?: IntersectionObserver;

  ngOnInit(): void {
    const cible = this.element.nativeElement;
    cible.classList.add('reveal');

    if (typeof IntersectionObserver === 'undefined') {
      cible.classList.add('reveal-visible');
      return;
    }

    this.observateur = new IntersectionObserver(
      (entrees) => {
        for (const entree of entrees) {
          if (entree.isIntersecting) {
            setTimeout(() => cible.classList.add('reveal-visible'), this.appRevealDelay());
            this.observateur?.disconnect();
          }
        }
      },
      { threshold: 0.15 },
    );
    this.observateur.observe(cible);
  }

  ngOnDestroy(): void {
    this.observateur?.disconnect();
  }
}
