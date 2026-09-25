# Journal de bord — 225

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que je viens de terminer
- **Bloqué** — ce qui m'a coûté du temps, et combien
- **IA** — ce que je lui ai demandé, et **comment j'ai vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges v1 (16 exigences fonctionnelles EF1–EF16, 15 règles de gestion RG1–RG15, 3 contradictions/trous tranchés en section 7), les quatre diagrammes (D1 cas d'utilisation en PlantUML, D2 modèle de données, D3 séquence, D4 états-transitions en Mermaid), contrat d'API complété (11 opérations), commit `[JALON] analyse` poussé.

**Bloqué :** 25 min sur la contradiction entre Q10 et Q15. Tranchée en faveur de Q15 : la note est verrouillée dès l'envoi — Q15 est postérieure dans l'échange et motivée par le client lui-même, Q11 décrit un usage réel mais ne porte pas sur ce point. Noté en section 7.

**IA :** m'a proposé un balayage systématique EF/RG contre le contrat qui a révélé 4 angles morts que je n'avais pas vus (429 absent pour EF4, `relecteurId` manquant rendant le 403 AUTO_RELECTURE invérifiable, aucun refus pour un dépôt sur session clôturée, `relecturesEnAttente` ambigu dans le tableau). Vérifié en reprenant chaque exigence une à une et en validant le YAML du contrat avec `js-yaml` avant de valider ses propositions.

---

## Étape 2 — Première version

**Fait :** EF2/EF3 (Must) livrée : `POST /api/presences` — présence enregistrée avec `source = ETUDIANT` (201), `410 CODE_EXPIRE` après la fenêtre de 15 min, `409 DEJA_PRESENT` (unicité base + service), `400 CODE_INCONNU`. Migration `V3__etudiants_et_presences.sql` (tables etudiant + presence, contrainte d'unicité session+étudiant). 3 tranchages nouveaux en section 7 : `404 ETUDIANT_INCONNU`, `409 SESSION_CLOTUREE` avec priorité du 410, normalisation du code (trim + casse). 8 tests d'intégration nouveaux, dont 2 avec horloge déplaçable pour le 410. 15/15 tests verts.

**Bloqué :** 20 min sur un bug de test subtil : remplacer le champ d'une `@TestConfiguration` ne change pas le `Clock` déjà injecté dans les services — résolu avec une horloge mutable déléguante (le bean lui-même change d'instant). Puis 10 min de `target/` verrouillé par l'instance de démo restée ouverte (leçon : tuer le process Java avant `mvn clean`).

**IA :** a proposé le découpage en 8 étapes, le format du code (alphabet sans ambiguïté visuelle), le bean `Clock` injectable et le test par horloge figée. Vérifié en relançant la suite Maven après chaque étape et en lisant les causes profondes dans les rapports surefire plutôt qu'en faisant confiance à ses explications.

### EF2/EF3 — présence par code

**Fait :** `POST /api/presences` — présence enregistrée avec `source = ETUDIANT` (201), `410 CODE_EXPIRE` après la fenêtre de 15 min, `409 DEJA_PRESENT` (unicité base + service), `400 CODE_INCONNU`. Migration `V3__etudiants_et_presences.sql` (tables etudiant + presence, contrainte d'unicité session+étudiant). 3 tranchages nouveaux en section 7 : `404 ETUDIANT_INCONNU`, `409 SESSION_CLOTUREE` avec priorité du 410, normalisation du code (trim + casse). 8 tests d'intégration nouveaux, dont 2 avec horloge déplaçable pour le 410. 15/15 tests verts.

**Bloqué :** 20 min sur un bug de test subtil : remplacer le champ d'une `@TestConfiguration` ne change pas le `Clock` déjà injecté dans les services — résolu avec une horloge mutable déléguante (le bean lui-même change d'instant). Puis 10 min de `target/` verrouillé par l'instance de démo restée ouverte (leçon : tuer le process Java avant `mvn clean`).

**IA :** a proposé l'ordre des vérifications (étudiant → code → clôturée → déjà présent), la priorité 410 > 409 et la normalisation du code. Vérifié en écrivant d'abord les tests qui encodent mes critères d'acceptation, en constatant moi-même les 2 échecs d'horloge, et en validant le fix par la relecture du mécanisme d'injection Spring (bean singleton).

### EF5 — présence ajoutée par le formateur

**Fait :** `POST /api/sessions/{id}/presences` — présence créée avec `source = FORMATEUR` (RG13), sans contrôle d'expiration du code, refusée si session inconnue (404), étudiant inconnu ou hors promotion (404), déjà présent (409, quelle que soit la source) ou session clôturée (409). Ajout de `GET /api/sessions/{id}/presences` pour rendre la source visible (critère « apparaît comme telle »). 2 tranchages nouveaux en section 7. 11 tests d'intégration nouveaux, 26/26 verts.

**Bloqué :**

**IA :**

### EF13/EF15 — clôture et réouverture manuelles

**Fait :** `PATCH /api/sessions/{id}/cloture` (OUVERTE → CLOTUREE, sinon `409 SESSION_DEJA_CLOTUREE`) et `PATCH /api/sessions/{id}/reouverture` (CLOTUREE → OUVERTE, sinon `409 SESSION_DEJA_OUVERTE`), `404 SESSION_INCONNUE` sur les deux. La réouverture ne touche pas `expirationAt` (RG15), vérifié par un test sur une session vieillie de 3h pour qu'un recalcul depuis l'horloge soit détecté. Test de bout en bout : présence manuelle refusée pendant la clôture, acceptée après réouverture. 8 tests nouveaux, 34/34 verts.

**Bloqué :**

**IA :**

### EF14 — auto-clôture 24h après expirationAt

**Fait :** tâche planifiée (`@Scheduled`, chaque minute) qui clôture en une requête les sessions `OUVERTE` dont `expirationAt + 24h` est atteint. Règle dans `SessionService` (testée à horloge déplaçable : 1 s avant la limite → reste ouverte, à la limite → clôturée, réouverture ne repousse pas le délai), tâche coupée en profil test pour éviter un thread de fond concurrent. Index V4 (statut, expiration_at). Horloge mutable extraite en classe de test partagée. 7 tests nouveaux, 41/41 verts.

**Bloqué :**

**IA :**

### EF6/EF7/EF8 — dépôt, remplacement et assignation du relecteur

**Fait :** `POST /api/exercices` (dépôt + tirage du relecteur parmi les présents hors auteur, `422` si personne) et `PUT /api/exercices/{id}` (remplacement par l'auteur tant que la relecture n'est pas rendue, même session clôturée). EF8 embarqué dans le ticket : le contrat et D4 assignent le relecteur au dépôt, un dépôt conforme est impossible sans. Migration V5 (exercice + relecture), paquetages `exercice/` et `relecture/`. 3 tranchages en section 7 (« démarrée » = rendue, `403 NON_AUTEUR`, auteur absent autorisé + définition du lien valide). D2 mis à jour (`verrouillee` retiré, redondant avec `rendueAt`). 18 tests nouveaux, 59/59 verts.

**Bloqué :**

**IA :**

### EF8 — assignation du relecteur (clôture du ticket)

**Fait :** code livré avec le dépôt (PR #17). Revue critère par critère : présence du relecteur et exclusion de l'auteur déjà testées ; « exactement un relecteur » (RG5) ne l'était pas — 2 tests ajoutés (une relecture par dépôt, doublon refusé par la contrainte en base). Migration de démo renumérotée V4 → V6 (collision de version avec l'index d'auto-clôture, Flyway aurait refusé de démarrer). 61/61 verts.

**Bloqué :**

**IA :**

### EF9/EF10/EF11 — rendu de la relecture

**Fait :** `POST /api/relectures/{id}` (note 0–20 + commentaire, exercice → `RELU`, note verrouillée) avec `403 AUTO_RELECTURE`, `409 RELECTURE_DEJA_RENDUE`, `400 NOTE_INVALIDE`, et `GET /api/relectures?relecteurId=`. Piège évité : une note déclarée `Integer` laisse Jackson tronquer `12.5` en `12` sans erreur — prouvé en repassant temporairement le champ en `Integer` (test en échec : 200 au lieu de 400), d'où la lecture en `BigDecimal`. Verrou d'écriture (`PESSIMISTIC_WRITE`) contre deux rendus simultanés. Au passage : identifiant non numérique et paramètre absent renvoyaient `500` partout, désormais `400`. 2 tranchages en section 7. 16 tests nouveaux, 77/77 verts.

**Bloqué :**

**IA :**

### EF12 — consultation de la note sans le relecteur

**Fait :** `GET /api/exercices/{id}` (id, lien, statut, note, commentaire ; note et commentaire nuls avant le rendu). RG7 vérifiée par la liste exacte des champs de la réponse plutôt que par l'absence d'un mot : tout champ ajouté plus tard fait échouer le test. Limite trouvée en vérifiant RG7 : sans authentification (Q1), `GET /api/relectures?relecteurId=` permet de deviner son relecteur en essayant tous les identifiants — documentée en section 7 comme limite v0.1. 6 tests nouveaux, 83/83 verts.

**Bloqué :**

**IA :**

---

## Étape 3 — Enveloppe

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
