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

### EF6/EF7/EF8 — dépôt, remplacement et assignation des relecteurs

**Fait :** ancienne implémentation de `POST /api/exercices` (dépôt + assignation d'un pair parmi les présents hors auteur, `422` si aucun) et de `PUT /api/exercices/{id}`. Cette règle a ensuite été remplacée par l'enveloppe du ticket #23 : deux pairs distincts sont désormais requis pour un nouveau dépôt. Migration V5 (exercice + relecture), paquetages `exercice/` et `relecture/`. 18 tests nouveaux, 59/59 verts.

**Bloqué :**

**IA :**

### EF8 — assignation des relecteurs (historique du ticket)

**Fait :** ancienne règle du ticket historique : un pair présent, distinct de l'auteur, était assigné au dépôt. Elle est remplacée par l'assignation de deux pairs du ticket #23/#24, avec conservation des relectures historiques lors de V7. Migration de démo renumérotée V4 → V6. 61/61 verts sur l'ancien périmètre.

**Bloqué :**

**IA :**

### EF9/EF10/EF11 — rendu de la relecture

**Fait :** ancienne implémentation de `POST /api/relectures/{id}` (note 0–20 + commentaire, exercice → `RELU`, note verrouillée), remplacée par deux rendus individuels : le premier reste provisoire et le second calcule la moyenne finale. Les contrôles `403 AUTO_RELECTURE`, `409 RELECTURE_DEJA_RENDUE`, `400 NOTE_INVALIDE` et le verrou d'écriture restent applicables à chaque relecture.

**Bloqué :**

**IA :**

### EF12 — consultation de la note sans le relecteur

**Fait :** ancienne réponse de `GET /api/exercices/{id}` avec une seule note et un seul commentaire. Le contrat révisé expose désormais une note provisoire ou la moyenne finale, ainsi que les commentaires rendus sans identité de relecteur. La limite d'anonymat liée à l'absence d'authentification reste documentée en section 7.

**Bloqué :**

**IA :**

### EF16 — tableau de suivi

**Fait :** `GET /api/tableau?promotionId=` — présences, exercices déposés, moyenne des notes retenues par exercice, relectures en attente, par étudiant. 5 requêtes agrégées quel que soit l'effectif (ENF2) ; moyenne calculée en Java depuis somme et nombre (AVG sur entier varie entre H2 et PostgreSQL), arrondie à 2 décimales (ENF3). Le score provisoire d'un exercice compte une fois jusqu'au second rendu. Scénario et assertions adaptés au double parcours.

**Bloqué :**

**IA :**

### Frontend — socle (avant les écrans)

**Fait :** analyse du frontend (EF1 et EF2 seuls branchés, relecteur vide, 8 tests de simple création). Socle posé : services + types pour toutes les opérations du contrat ; sélecteur d'identité « Je suis » (+ `GET /api/etudiants`) à la place des `etudiantId: 1` / `promotionId: 1` en dur ; 26 codes d'erreur traduits FR/EN (pipe `erreurApi`). Bugs corrigés : route `**` qui redirigeait vers une route inexistante, erreurs affichées brutes ou génériques. Tests frontend 8 → 24 (conformité de chaque URL au contrat, identité envoyée, erreur traduite de bout en bout avec le vrai intercepteur) ; backend 92/92.

**Bloqué :**

**IA :**

---

## Étape 3 — Enveloppe

**Fait :** ticket #23 ouvert avant le développement. Analyse mise à jour et commit de conséquence `cd09f0b`. Découpage proposé en trois sous-issues : migration et conservation des données ; assignation/rendu des deux pairs ; contrat API et interfaces. Le propriétaire du dépôt ouvre ces sous-issues GitHub. Sacrifice initialement inscrit sur EF14 (auto-clôture), puis corrigé : EF14 était déjà livrée et testée en v0.1, la reporter ne libérait rien et laissait un code actif contredisant le cahier des charges. Le sacrifice porte sur EF4 (voir ci-dessous).

**Bloqué :** un autre éditeur a réécrit certains fichiers backend en parallèle, réintroduisant la règle « RELU au premier rendu ». Le travail de stabilisation continue après coordination.

**IA :** l'analyse repérée par ticket #23 a conduit à retenir le rang de chaque relecture pour migrer l'historique sans écraser les notes existantes. Vérification en cours par migrations Flyway et tests d'intégration du cas un puis deux rendus.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** EF4 (blocage 2 min après 5 codes erronés, Should), reportée à v1.1. C'est le seul travail restant qui n'était pas encore développé : le temps prévu pour lui passe à la double relecture, un Must qui touche la note de chaque étudiant. EF4 est une protection anti-abus secondaire ; son absence n'empêche aucun parcours.

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
