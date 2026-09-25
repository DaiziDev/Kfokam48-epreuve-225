import { TestBed } from '@angular/core/testing';

import { Relecteur } from './relecteur';

describe('Relecteur', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Relecteur],
    }).compileComponents();
  });

  it('se cree', () => {
    const fixture = TestBed.createComponent(Relecteur);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche le titre des relectures', () => {
    const fixture = TestBed.createComponent(Relecteur);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Mes relectures');
  });
});
