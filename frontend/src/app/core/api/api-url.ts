import { InjectionToken } from '@angular/core';

/**
 * URL de base de l'API, relative par défaut : en développement le proxy
 * (proxy.conf.json) la renvoie vers le backend, en production l'app est
 * servie derrière la même origine. Aucune URL codée en dur dans les services.
 */
export const API_URL = new InjectionToken<string>('API_URL', {
  factory: () => '/api',
});
