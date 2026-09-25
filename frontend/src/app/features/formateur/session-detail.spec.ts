import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { SessionDetail } from './session-detail';

describe('SessionDetail', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [SessionDetail],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('actualise les présences pendant une session ouverte', () => {
    const fixture = TestBed.createComponent(SessionDetail);
    fixture.componentRef.setInput('session', {
      id: 7,
      titre: 'Algorithmique',
      code: 'ABC234',
      ouvertureAt: '2026-09-25T10:00:00Z',
      expirationAt: '2099-09-25T10:15:00Z',
      statut: 'OUVERTE',
    });
    fixture.detectChanges();

    http.expectOne('/api/etudiants?promotionId=1').flush([
      { id: 1, nom: 'Alice' },
      { id: 2, nom: 'Boris' },
    ]);
    http.expectOne('/api/sessions/7/presences').flush([
      { id: 11, etudiantId: 1, nom: 'Alice', source: 'ETUDIANT', marqueeAt: '2026-09-25T10:01:00Z' },
    ]);
    fixture.detectChanges();
    const listeInitiale = (fixture.nativeElement as HTMLElement).querySelector('[data-test="presents"]');
    expect(listeInitiale?.textContent).toContain('Alice');
    expect(listeInitiale?.textContent).not.toContain('Boris');

    vi.advanceTimersByTime(5000);
    http.expectOne('/api/sessions/7/presences').flush([
      { id: 11, etudiantId: 1, nom: 'Alice', source: 'ETUDIANT', marqueeAt: '2026-09-25T10:01:00Z' },
      { id: 12, etudiantId: 2, nom: 'Boris', source: 'ETUDIANT', marqueeAt: '2026-09-25T10:01:01Z' },
    ]);
    fixture.detectChanges();

    const listeActualisee = (fixture.nativeElement as HTMLElement).querySelector('[data-test="presents"]');
    expect(listeActualisee?.textContent).toContain('Alice');
    expect(listeActualisee?.textContent).toContain('Boris');
    fixture.destroy();
  });
});
