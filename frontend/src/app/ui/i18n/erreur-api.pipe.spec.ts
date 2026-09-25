import { TestBed } from '@angular/core/testing';

import { ApiError } from '../../core/api/erreur-api';
import { ErreurApiPipe } from './erreur-api.pipe';
import { I18nService } from './i18n.service';

describe('ErreurApiPipe', () => {
  let pipe: ErreurApiPipe;
  let i18n: I18nService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ErreurApiPipe] });
    pipe = TestBed.inject(ErreurApiPipe);
    i18n = TestBed.inject(I18nService);
  });

  it('traduit un code du contrat dans la langue courante', () => {
    const erreur = new ApiError('CODE_EXPIRE', 'Le code de présence a expiré.', 410);
    expect(pipe.transform(erreur)).toContain('15 minutes');

    i18n.basculer();
    expect(pipe.transform(erreur)).toContain('15-minute window');
  });

  it('retombe sur le message du serveur pour un code inconnu du frontend', () => {
    const erreur = new ApiError('CODE_FUTUR', 'Message précis du serveur.', 400);
    expect(pipe.transform(erreur)).toBe('Message précis du serveur.');
  });

  it('retombe sur le message générique, jamais sur une clé brute', () => {
    const erreur = new ApiError('CODE_FUTUR', '', 500);
    expect(pipe.transform(erreur)).toBe('Une erreur inattendue est survenue.');
    expect(pipe.transform(null)).toBe('');
  });
});
