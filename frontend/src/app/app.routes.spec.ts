import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { routes } from './app.routes';

describe('Routes', () => {
  it('une URL inconnue ramène à l accueil au lieu de boucler', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/page-qui-n-existe-pas');

    expect(router.url).toBe('/');
  });
});
