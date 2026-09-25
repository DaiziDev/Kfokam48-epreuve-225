# CHANGELOG

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnement [Sémantique](https://semver.org/lang/fr/).

## [Non publié]

### Ajouté
- **Design system frontend** (benchmark documenté dans `docs/DESIGN.md`) : tokens Tailwind 4 (`@theme`) — indigo de marque, statuts sémantiques emerald/amber/red, typo Inter + mono ; composants `ui-icon` (SVG Lucide inlinés, zéro emoji, zéro dépendance), `ui-badge-statut`, `ui-code-input` (6 cases, collage, navigation clavier, CVA) ; i18n FR/EN par signals avec bascule instantanée.
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
