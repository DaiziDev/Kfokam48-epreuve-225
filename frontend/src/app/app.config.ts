import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { API_URL } from './core/api/api-url';
import { intercepteurErreurs } from './core/api/intercepteur-erreurs';

/**
 * SPA classique (pas de SSR) : app métier interne, aucun SEO nécessaire.
 * La couche API est fournie ici une fois pour toutes ; les composants
 * n'utilisent HttpClient qu'au travers des services de core/.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([intercepteurErreurs])),
    { provide: API_URL, useValue: '/api' },
  ],
};
