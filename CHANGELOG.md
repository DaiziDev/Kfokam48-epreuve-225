# CHANGELOG

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnement [Sémantique](https://semver.org/lang/fr/).

## [Non publié]

### Ajouté — dockerisation
- `docker compose up --build` lance l'application complète sur http://localhost:8090 : PostgreSQL 17 (volume persistant), API Spring Boot (image JRE 21, utilisateur non-root, migrations Flyway au démarrage) et nginx (SPA Angular + relais `/api` et Swagger UI vers l'API). Démarrage ordonné par contrôles de santé : l'API attend la base, nginx attend l'API. Seul nginx est exposé ; port et identifiants surchargeables par variables d'environnement.
- README : démarrage Docker ; corrections de l'installation manuelle (API sur le port 8081 et non 8080, frontend SPA et non SSR, chemin du journal).

### Modifié — analyse du ticket #23 (double relecture)
- Cahier des charges révisé : RG5 (deux relecteurs distincts, auteur exclu), RG6, RG10, RG12, nouvelle RG16 (note retenue = moyenne des deux rendus, provisoire après un seul) ; EF7, EF8, EF9, EF12, EF16 réécrites ; comportement explicite avec moins de deux relecteurs éligibles (`422 AUCUN_RELECTEUR_DISPONIBLE`). Diagrammes D1, D2, D4 mis à jour.
- Contrat API révisé : `noteProvisoire` et `commentaires` (anonymes) sur `GET /api/exercices/{id}`, `moyenneProvisoire` sur `GET /api/tableau`.
- Périmètre : EF4 (blocage après 5 codes erronés, Should, non développée) est reportée à v1.1 pour absorber ce Must tardif ; EF14 (auto-clôture), déjà livrée en v0.1, reste en place.

### Ajouté
- **Design system frontend** (benchmark documenté dans `docs/DESIGN.md`) : tokens Tailwind 4 (`@theme`) — indigo de marque, statuts sémantiques emerald/amber/red, typo Inter + mono ; composants `ui-icon` (SVG Lucide inlinés, zéro emoji, zéro dépendance), `ui-badge-statut`, `ui-code-input` (6 cases, collage, navigation clavier, CVA) ; i18n FR/EN par signals avec bascule instantanée.
- **Socle frontend** : couche API complète (services et types pour toutes les opérations du contrat, testés verbe + chemin + corps) ; sélecteur d'identité « Je suis » (fin des `etudiantId: 1` / `promotionId: 1` en dur) ; 26 codes d'erreur du contrat traduits FR/EN via le pipe `erreurApi`.
- `GET /api/etudiants?promotionId=` : étudiants d'une promotion triés par nom, pour le sélecteur d'identité (ajout additif, section 7).

### Corrigé
- Frontend : une URL inconnue redirigeait vers une route `accueil` inexistante ; elle ramène désormais à l'accueil.
- Frontend : les erreurs de l'API s'affichaient brutes en français (écran étudiant) ou toujours génériques (écran formateur) ; elles sont traduites depuis leur code dans la langue courante.

### Ajouté (suite)
- **EF16 (Must)** — `GET /api/tableau?promotionId=` : par étudiant de la promotion, `presences` (codes et ajouts du formateur), `exercicesDeposes`, `moyenne` des notes reçues (arrondie à 2 décimales, `null` sans note — calculée côté API, ENF3) et `relecturesEnAttente` (relectures assignées non rendues). Étudiants sans activité inclus, tri par nom. Nombre de requêtes fixe (5 agrégats), quel que soit l'effectif (ENF2). `404 PROMOTION_INCONNUE`.
- **EF12 (Must)** — `GET /api/exercices/{id}` : l'étudiant consulte son exercice (`id`, `lien`, `statut`, `note`, `commentaire`) ; note et commentaire nuls tant que la relecture n'est pas rendue. Aucun champ identifiant le relecteur (RG7) : la liste exacte des champs est vérifiée par test. `404 EXERCICE_INCONNU`. Limite RG7 sans authentification documentée en section 7.
- **EF9/EF10/EF11 (Must)** — `POST /api/relectures/{id}` : le relecteur assigné rend une note entière 0–20 et un commentaire (200), l'exercice passe à `RELU` et la note est verrouillée (RG9). `400 NOTE_INVALIDE` (hors bornes ou non entière — note lue en décimal exact pour que 12.5 ne soit pas tronqué) ; `403 AUTO_RELECTURE` (RG4) ; `403 RELECTEUR_NON_ASSIGNE` (section 7) ; `409 RELECTURE_DEJA_RENDUE` ; `404 RELECTURE_INCONNUE` ; `400 COMMENTAIRE_INVALIDE` au-delà de 4000 caractères. Verrou d'écriture sur la relecture : deux rendus simultanés ne peuvent pas écraser la note.
- `GET /api/relectures?relecteurId=` (EF9) : relectures assignées, à faire ou rendues, avec le lien de l'exercice ; `404 ETUDIANT_INCONNU`.
- Erreurs de paramètres : `400 CHAMPS_REQUIS` pour un paramètre de requête absent, `400 PARAMETRE_INVALIDE` pour un identifiant non numérique — ces deux cas renvoyaient `500` sur tous les endpoints.
- **EF8 (Must)** — critères d'acceptation vérifiés un à un : RG5 (exactement un relecteur par exercice) désormais testé, y compris le refus en base d'un second relecteur (`uq_relecture_exercice`). Assignation livrée avec le dépôt (voir EF6/EF8 ci-dessous).
- Migration `V6__etudiants_demo.sql` : trois étudiants de démonstration dans la promotion KFOKAM48 (ENF2).
- **EF6/EF8 (Must)** — `POST /api/exercices` : l'étudiant dépose le lien de son exercice (201, statut `EN_ATTENTE_RELECTURE`) ; un relecteur est tiré au hasard parmi les présents de la session, auteur exclu (RG4, RG5, RG6), jamais exposé dans la réponse. `422 AUCUN_RELECTEUR_DISPONIBLE` s'il n'y a aucun autre présent ; `409 EXERCICE_DEJA_DEPOSE` ; `409 SESSION_CLOTUREE` (RG11) ; `404 SESSION_INCONNUE` / `ETUDIANT_INCONNU` ; `400 LIEN_INVALIDE` (URL http(s) absolue exigée).
- **EF7 (Must)** — `PUT /api/exercices/{id}` : l'auteur remplace son lien tant que la relecture n'est pas rendue, même après clôture de la session (RG12) ; relecteur inchangé. `409 RELECTURE_DEJA_COMMENCEE` ; `403 NON_AUTEUR` (ajout au contrat, section 7) ; `404 EXERCICE_INCONNU`.
- Migration `V5__exercices_et_relectures.sql` : tables `exercice` (unicité session + étudiant) et `relecture` (unicité par exercice pour RG5, note contrainte à 0–20 pour RG8).
- **EF14 (Must)** — auto-clôture : une tâche planifiée (chaque minute, `kfokam48.auto-cloture.intervalle`) passe à `CLOTUREE` toute session `OUVERTE` dont `expirationAt + 24h` est atteint (RG14). Le délai part de l'`expirationAt` d'origine, réouverture comprise (RG15). Tâche désactivable (`kfokam48.auto-cloture.active`), coupée en profil test. Migration `V4__index_auto_cloture.sql` : index (statut, expiration_at).
- **EF13/EF15 (Must)** — `PATCH /api/sessions/{id}/cloture` et `PATCH /api/sessions/{id}/reouverture` : le formateur clôture une session `OUVERTE` (200 `CLOTUREE`) ou rouvre une session `CLOTUREE` (200 `OUVERTE`) à tout moment (RG15). `409 SESSION_DEJA_CLOTUREE` / `409 SESSION_DEJA_OUVERTE` ; `404 SESSION_INCONNUE`. La réouverture ne modifie ni `ouvertureAt` ni `expirationAt` : le code expiré le reste, le délai de 24h (RG14) reste calculé depuis la date d'origine.
- **EF5 (Must)** — `POST /api/sessions/{id}/presences` : le formateur ajoute la présence d'un étudiant qui n'a pas pu saisir le code. 201 avec `source = FORMATEUR` (RG13) ; possible après expiration du code tant que la session est `OUVERTE` ; `404 SESSION_INCONNUE` ; `404 ETUDIANT_INCONNU` (inexistant ou hors promotion, décision section 7) ; `409 DEJA_PRESENT` quelle que soit la source de la première présence ; `409 SESSION_CLOTUREE` ; `400 CHAMPS_REQUIS`.
- `GET /api/sessions/{id}/presences` (ajout additif, décision section 7) : présents d'une session avec `nom`, `source` et `marqueeAt`, pour distinguer les ajouts du formateur.
- **EF2/EF3 (Must)** — `POST /api/presences` : l'étudiant marque sa présence avec le code. 201 avec `source = ETUDIANT` ; `410 CODE_EXPIRE` (fenêtre RG1 passée) ; `409 DEJA_PRESENT` ; `400 CODE_INCONNU` ; plus `404 ETUDIANT_INCONNU` et `409 SESSION_CLOTUREE` (décisions section 7). Code normalisé (trim + casse) pour la saisie manuelle.
- Migration `V3__etudiants_et_presences.sql` : tables `etudiant` et `presence`, contrainte d'unicité (session, étudiant).
- Architecture frontend : SPA Angular sans SSR, couche API isolée (`core/api/` : token `API_URL`, intercepteur d'erreurs normalisant en `{ code, message }`, service par ressource du contrat), types TS miroir du contrat, 3 écrans lazy (`etudiant/`, `relecteur/`, `formateur/`), proxy de dev vers le backend. Documentée dans `docs/ARCHITECTURE.md`.
- `docs/ARCHITECTURE.md` : décisions backend/frontend documentées, alternatives rejetées.
- Swagger UI (springdoc 3.1.1) : `/swagger-ui.html`, métadonnées issues du contrat.
- Migration `V2__promotion_demo.sql` : promotion KFOKAM48 de démonstration (ENF2).
- Gestion du JSON malformé : 400 `CORPS_INVALIDE` au lieu du 500 générique.
- **EF1 (Must)** — `POST /api/sessions` : le formateur ouvre une session et obtient un code de présence. 201 avec `id, code, ouvertureAt, expirationAt, statut` ; `expirationAt = ouvertureAt + 15 min` (RG1) ; 400 `CHAMPS_REQUIS` si champ manquant ; 404 `PROMOTION_INCONNUE` (décision section 7).
- Migration Flyway `V1__sessions_et_promotions.sql` : tables `promotion` et `session_cours` en `BIGINT IDENTITY`, code de session unique indexé.

## [0.1.0] — analyse

### Ajouté
- Cahier des charges v1 : 16 exigences fonctionnelles (EF1–EF16), 15 règles de gestion (RG1–RG15), exigences non fonctionnelles, contraintes techniques et démarche.
- Section 7 « Zones d'ombre, hypothèses et contradictions tranchées » : 9 décisions documentées, dont la retenue de Q15 contre Q10 (note verrouillée dès l'envoi).
- Diagrammes versionnés : D1 cas d'utilisation (PlantUML), D2 modèle de données, D3 séquence « marquer sa présence », D4 états-transitions de l'exercice (Mermaid).
- Contrat d'API complété (`api/contrat.yml`) : 11 opérations sur 10 chemins — sessions (création, clôture, réouverture), présence par code, présence manuelle du formateur, dépôt et remplacement et consultation d'exercice, rendu et liste des relectures, tableau de suivi.
- Codes d'erreur métier au format imposé `{ code, message }` : `410 CODE_EXPIRE`, `409 DEJA_PRESENT`, `409 RELECTURE_DEJA_COMMENCEE`, `403 AUTO_RELECTURE`, `429 TROP_ECHECS`, `422 AUCUN_RELECTEUR_DISPONIBLE`, entre autres.
- Modèle de données aligné sur le contrat : clés primaires `bigint` auto-incrément générées par la base (`BIGSERIAL`), jamais d'UUID applicatif.
- Livrables documentaires : `README.md`, `CHANGELOG.md`, `JOURNAL.md`.

[Non publié]: https://github.com/kfokam48/kfokam48-epreuve-225/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/kfokam48/kfokam48-epreuve-225/tags/v0.1.0
