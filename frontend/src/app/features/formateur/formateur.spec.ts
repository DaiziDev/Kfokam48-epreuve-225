import { TestBed } from '@angular/core/testing';

import { Formateur } from './formateur';

describe('Formateur', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Formateur],
    }).compileComponents();
  });

  it('se cree', () => {
    const fixture = TestBed.createComponent(Formateur);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche le titre de l espace formateur', () => {
    const fixture = TestBed.createComponent(Formateur);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Espace formateur');
  });
});
