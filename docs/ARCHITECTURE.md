# Architecture

## Vue d'ensemble

```
┌───────────────┐   /api (proxy dev)   ┌──────────────────────────────┐
│  Frontend SPA │ ───────────────────► │  Backend Spring Boot en      │
│  Angular 21   │                      │  couches, API REST versionnée│
└───────────────┘                      └──────────────────────────────┘
     fichiers                                    │
     statiques                                   ▼
                                        PostgreSQL (prod) / H2 (tests)
                                        schéma = migrations Flyway
```

Le frontend est une **SPA classique** (pas de SSR) : app métier interne, aucun SEO à
satisfaire, déploiement en fichiers statiques. Toute la règle métier vit dans le
backend (ENF3) : le frontend affiche, il ne calcule rien.

## Backend

### Couches (imposées par le cahier des charges §8)

```
Controller (DTO validés) → Service (règles métier) → Repository (JPA) → Flyway (schéma)
```

- **DTO obligatoires** : jamais d'entité JPA exposée directement — c'est aussi le
  rempart de RG7 (pas de champ `relecteurId` qui fuite vers l'étudiant relu).
- **Gestion d'erreurs centralisée** : `@RestControllerAdvice` (`api/GestionErreursAdvice`)
  → toutes les erreurs au format imposé `{ code, message }`, y compris JSON malformé
  (400 `CORPS_INVALIDE` au lieu du 500 par défaut).
- **`Clock` injectable** (`conf/HorlogeConfiguration`) : RG1 (+15 min), puis RG3
  (blocage 2 min) et RG14 (auto-clôture 24h) calculent le temps via ce bean — testable
  par horloge figée, sans attendre ni mocker.

### Organisation des paquetages

Paquetages **par domaine** puis techniques — un ticket EFx ne touche qu'un paquetage :

```
com.kfokam48.kfokam48
├── api/         GestionErreursAdvice (transverse)
├── conf/        Clock, OpenAPI (transverse)
├── session/     sessions, étudiants, présences : contrôleurs, services, entités, repos, exceptions
├── exercice/    dépôt et remplacement du lien, tirage du relecteur (EF6–EF8)
├── relecture/   rendu de la relecture, liste des relectures d'un relecteur (EF9–EF11)
└── tableau/     tableau de suivi (EF16) : lecture seule, agrégats JPQL via EntityManager
```

Les présences sont restées dans `session/` (fortement liées au code de session).
`tableau/` ne possède aucune entité : il agrège celles des autres domaines en 5 requêtes
`GROUP BY` fixes (ENF2) et calcule la moyenne côté API (ENF3).

### Base de données

- Identifiants **`BIGINT IDENTITY`** générés par la base (alignés sur le contrat int64,
  jamais d'UUID applicatif — cf. D2).
- **Flyway versionné** : V1 promotion + session_cours, V2 promotion de démonstration,
  V3 etudiant + presence, V4 index de l'auto-clôture, V5 exercice + relecture. Une migration par ticket,
  l'historique raconte les stories.

### Tâches planifiées

- **Auto-clôture (RG14)** : `session/AutoClotureTache` appelle chaque minute
  `SessionService.cloturerSessionsEchues()`. La règle reste dans le service (testable
  par horloge déplaçable) ; la tâche ne fait que la déclencher.
- Coupée en profil test (`kfokam48.auto-cloture.active=false`) : pas de thread de fond
  qui modifie les données pendant qu'un test les vérifie.

## Frontend

### Structure

```
src/app
├── core/                    couche technique isolée (§8 : « appels API isolés »)
│   ├── api/
│   │   ├── api-url.ts            InjectionToken /api (aucune URL en dur)
│   │   ├── erreur-api.ts         ApiError { code, message } = format du contrat
│   │   ├── intercepteur-erreurs.ts  toute erreur → ApiError, une seule forme
│   │   └── sessions/…            un service par ressource du contrat
│   ├── api/promotion.ts      PROMOTION_COURANTE (promotion de démo, un seul endroit)
│   ├── sessions/ presences/ exercices/ relectures/ tableau/ etudiants/
│   │                         un service par ressource — testés contre le contrat (services-api.spec.ts)
│   ├── identite/            « qui suis-je » : choisi dans une liste (auth hors périmètre, Q1)
│   └── types/               types TS miroir de api/contrat.yml
├── features/                un dossier par écran, lazy loading
│   ├── etudiant/            (EF2–EF8, EF12 : code, dépôt, consultation)
│   ├── relecteur/           (EF9–EF12 : relectures assignées, rendu)
│   └── formateur/           (EF1, EF13–EF16 : session, tableau)
└── app.ts / app.html        coquille : navigation 3 rôles, aucun métier
```

### Règles

1. **Un composant n'injecte jamais `HttpClient`** : il passe par un service de `core/`.
2. **Un service n'écrit jamais d'URL à la main** : il injecte `API_URL`.
3. **Les types viennent de `core/types/`** (miroir du contrat), jamais de `any`.
4. **Aucun recalcul métier** (ENF3) : moyenne, statuts et compteurs viennent de l'API ;
   le frontend ne fait que les afficher.
5. **Mobile-first** (ENF1) : Tailwind, coquille testée à 375px, chaque écran dans un
   conteneur `max-w-*` avec padding.

### Développement

```bash
npm start        # ng serve + proxy /api → http://localhost:8081
npm test         # vitest
npm run build    # bundle statique
```

Le proxy de développement (`proxy.conf.json`) pointe vers le backend ; en production,
l'app statique est servie derrière la même origine que l'API.

## Décisions rejetées (et pourquoi)

| Alternative | Raison du rejet |
|---|---|
| SSR Angular (scaffold par défaut) | Aucun SEO requis pour une app de formation interne ; hydratation et serveur Express = complexité sans bénéfice |
| NgRx/signals store global | 3 écrans indépendants sans état partagé réel ; un service par ressource suffit à v0.1 — à réévaluer si un état transverse apparaît |
| UUID applicatifs | Contrat imposé en int64 ; identité gérée par la base |
| Recalcul de la moyenne côté client | Interdit par ENF3 |
