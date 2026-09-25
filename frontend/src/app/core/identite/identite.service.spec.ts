import { TestBed } from '@angular/core/testing';

import { IdentiteService } from './identite.service';

describe('IdentiteService', () => {
  beforeEach(() => localStorage.clear());

  it('aucune identité au départ', () => {
    const service = TestBed.inject(IdentiteService);
    expect(service.etudiantId()).toBeNull();
  });

  it('retient l identité choisie entre deux visites', () => {
    TestBed.inject(IdentiteService).choisir({ id: 2, nom: 'Boris T.' });

    // Nouvelle visite : nouvelle instance, relue depuis le stockage
    TestBed.resetTestingModule();
    const relu = TestBed.inject(IdentiteService);
    expect(relu.etudiant()).toEqual({ id: 2, nom: 'Boris T.' });
  });

  it('ignore une valeur stockée corrompue', () => {
    localStorage.setItem('kfokam48-identite', '{pas du json');
    expect(TestBed.inject(IdentiteService).etudiantId()).toBeNull();
  });

  it('oublie l identité quand on la retire', () => {
    const service = TestBed.inject(IdentiteService);
    service.choisir({ id: 2, nom: 'Boris T.' });
    service.choisir(null);
    expect(service.etudiantId()).toBeNull();
    expect(localStorage.getItem('kfokam48-identite')).toBeNull();
  });
});
