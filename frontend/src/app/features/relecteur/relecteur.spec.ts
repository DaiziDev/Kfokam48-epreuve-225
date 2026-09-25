import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Relecteur } from './relecteur';

describe('Relecteur', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Relecteur],
      providers: [provideHttpClient(), provideHttpClientTesting()],
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
    expect(element.querySelector('h1')?.textContent).toContain('Vos relectures assignées');
  });
});
