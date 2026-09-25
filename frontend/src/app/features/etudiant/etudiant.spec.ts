import { TestBed } from '@angular/core/testing';

import { Etudiant } from './etudiant';

describe('Etudiant', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Etudiant],
    }).compileComponents();
  });

  it('se cree', () => {
    const fixture = TestBed.createComponent(Etudiant);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche le titre de l espace étudiant', () => {
    const fixture = TestBed.createComponent(Etudiant);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Espace étudiant');
  });
});
