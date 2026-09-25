import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { intercepteurErreurs } from '../../core/api/intercepteur-erreurs';
import { IdentiteService } from '../../core/identite/identite.service';
import { Etudiant } from './etudiant';

describe('Etudiant', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Etudiant],
      // Le vrai intercepteur, comme dans app.config : l'erreur HTTP devient une ApiError
      providers: [provideHttpClient(withInterceptors([intercepteurErreurs])), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  /** Le sélecteur d'identité charge la liste de la promotion au démarrage. */
  function demarrer() {
    const fixture = TestBed.createComponent(Etudiant);
    fixture.detectChanges();
    http.expectOne('/api/etudiants?promotionId=1').flush([
      { id: 1, nom: 'Alice K.' },
      { id: 2, nom: 'Boris T.' },
    ]);
    fixture.detectChanges();
    return fixture;
  }

  function boutonValider(element: HTMLElement): HTMLButtonElement {
    return element.querySelector('button.btn-primary') as HTMLButtonElement;
  }

  it('affiche le titre de l écran étudiant', () => {
    const element = demarrer().nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Marquez votre présence');
  });

  it('propose les étudiants de la promotion dans le sélecteur', () => {
    const element = demarrer().nativeElement as HTMLElement;
    const options = Array.from(element.querySelectorAll('#identite option')).map((o) => o.textContent?.trim());
    expect(options).toEqual(['Choisir mon nom…', 'Alice K.', 'Boris T.']);
  });

  it('refuse de valider tant qu aucune identité n est choisie', () => {
    const fixture = demarrer();
    fixture.componentInstance.surCodeChange('ABC234');
    fixture.detectChanges();

    expect(boutonValider(fixture.nativeElement).disabled).toBe(true);
    fixture.componentInstance.valider();
    http.expectNone('/api/presences');
  });

  it('envoie l identité choisie, plus jamais un etudiantId en dur', () => {
    const fixture = demarrer();
    TestBed.inject(IdentiteService).choisir({ id: 2, nom: 'Boris T.' });
    fixture.componentInstance.surCodeChange('ABC234');
    fixture.detectChanges();

    expect(boutonValider(fixture.nativeElement).disabled).toBe(false);
    fixture.componentInstance.valider();

    const requete = http.expectOne('/api/presences');
    expect(requete.request.body).toEqual({ code: 'ABC234', etudiantId: 2 });
  });

  it('affiche l erreur traduite depuis le code du contrat', () => {
    const fixture = demarrer();
    TestBed.inject(IdentiteService).choisir({ id: 2, nom: 'Boris T.' });
    fixture.componentInstance.surCodeChange('ABC234');
    fixture.componentInstance.valider();

    http.expectOne('/api/presences').flush(
      { code: 'CODE_EXPIRE', message: 'Le code de présence a expiré.' },
      { status: 410, statusText: 'Gone' },
    );
    fixture.detectChanges();

    const alerte = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerte?.textContent).toContain('la fenêtre de 15 minutes est passée');
  });
});
