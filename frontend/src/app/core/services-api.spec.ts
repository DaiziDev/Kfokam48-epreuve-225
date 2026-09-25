import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EtudiantsApiService } from './etudiants/etudiants-api.service';
import { ExercicesApiService } from './exercices/exercices-api.service';
import { PresencesApiService } from './presences/presences-api.service';
import { RelecturesApiService } from './relectures/relectures-api.service';
import { SessionsApiService } from './sessions/sessions-api.service';
import { TableauApiService } from './tableau/tableau-api.service';

/**
 * Chaque méthode de la couche API contre api/contrat.yml : verbe, chemin,
 * paramètres et corps. Une faute de frappe dans une URL échoue ici, pas en démo.
 */
describe('Couche API — conformité au contrat', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function attendre(methode: string, url: string, corps?: unknown): void {
    const requete = http.expectOne((r) => r.method === methode && r.urlWithParams === url);
    if (corps !== undefined) {
      expect(requete.request.body).toEqual(corps);
    }
    requete.flush({});
  }

  it('sessions : ouverture, clôture, réouverture, présences manuelles', () => {
    const api = TestBed.inject(SessionsApiService);
    api.ouvrirSession({ titre: 'Algo', promotionId: 1 }).subscribe();
    attendre('POST', '/api/sessions', { titre: 'Algo', promotionId: 1 });
    api.cloturer(7).subscribe();
    attendre('PATCH', '/api/sessions/7/cloture');
    api.rouvrir(7).subscribe();
    attendre('PATCH', '/api/sessions/7/reouverture');
    api.ajouterPresence(7, 3).subscribe();
    attendre('POST', '/api/sessions/7/presences', { etudiantId: 3 });
    api.listerPresences(7).subscribe();
    attendre('GET', '/api/sessions/7/presences');
  });

  it('présences : marquage par code', () => {
    TestBed.inject(PresencesApiService).marquerPresence({ code: 'ABC234', etudiantId: 3 }).subscribe();
    attendre('POST', '/api/presences', { code: 'ABC234', etudiantId: 3 });
  });

  it('exercices : dépôt, remplacement, consultation', () => {
    const api = TestBed.inject(ExercicesApiService);
    api.deposer({ sessionId: 7, etudiantId: 3, lien: 'https://x.io' }).subscribe();
    attendre('POST', '/api/exercices', { sessionId: 7, etudiantId: 3, lien: 'https://x.io' });
    api.remplacerLien(9, { etudiantId: 3, lien: 'https://y.io' }).subscribe();
    attendre('PUT', '/api/exercices/9', { etudiantId: 3, lien: 'https://y.io' });
    api.consulter(9).subscribe();
    attendre('GET', '/api/exercices/9');
  });

  it('relectures : liste et rendu', () => {
    const api = TestBed.inject(RelecturesApiService);
    api.lister(3).subscribe();
    attendre('GET', '/api/relectures?relecteurId=3');
    api.rendre(5, { relecteurId: 3, note: 15, commentaire: 'Bien.' }).subscribe();
    attendre('POST', '/api/relectures/5', { relecteurId: 3, note: 15, commentaire: 'Bien.' });
  });

  it('tableau et étudiants : filtrés par promotion', () => {
    TestBed.inject(TableauApiService).consulter(1).subscribe();
    attendre('GET', '/api/tableau?promotionId=1');
    TestBed.inject(EtudiantsApiService).lister(1).subscribe();
    attendre('GET', '/api/etudiants?promotionId=1');
  });
});
