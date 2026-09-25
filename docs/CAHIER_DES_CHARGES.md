# Cahier des charges — KFOKAM48 Présence & Relecture

Auteur : 225 · Version 1 · Frontend choisi : Angular

## 1. Contexte et objectif

La direction de la formation KFOKAM48 gère des sessions de cours en présentiel. Elle a besoin de fiabiliser trois choses qui se font aujourd'hui de façon informelle : la prise de présence, le dépôt des exercices, et leur relecture par les pairs. L'application remplace ce suivi manuel par un système où chaque session génère un code de présence temporaire, chaque étudiant dépose son exercice en ligne, et les relectures sont attribuées automatiquement entre pairs présents. Le formateur dispose d'un tableau de suivi par étudiant et par session.

## 2. Acteurs et rôles

| Acteur    | Ce qu'il peut faire                                                                                                                                                                    |
| --------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Formateur | Ouvre une session, obtient le code de présence, clôture ou rouvre une session, ajoute une présence manuelle, consulte le tableau de suivi                                              |
| Étudiant  | Saisit un code pour marquer sa présence, dépose le lien de son exercice, effectue une relecture qui lui est assignée, consulte la note et le commentaire reçus sur son propre exercice |
| Système   | Choisit aléatoirement le relecteur parmi les étudiants présents, applique l'auto-clôture après 24h                                                                                     |

## 3. Périmètre

**Inclus** :

1. Gestion des sessions
2. Code de présence à durée limitée
3. Présence étudiante et présence ajoutée par le formateur
4. Dépôt et remplacement de lien d'exercice
5. Attribution aléatoire d'un relecteur
6. Saisie de note et commentaire
7. Tableau de suivi par étudiant.

**Exclu** :

1. Authentification par mot de passe (Q1)
2. Gestion des promotions et des inscriptions (supposée préexistante, l'API reçoit un `promotionId`)
3. Notifications (email, push)
4. Consultation de l'identité du relecteur par l'étudiant relu (Q8)
5. Gestion des groupes ou classes autres que la promotion.

## 4. Exigences fonctionnelles

| Réf  | Exigence                                                                   | Critère d'acceptation                                                                                                                                                                      | Priorité |
| ---- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| EF1  | Le formateur ouvre une session et obtient un code de présence              | Quand je crée une session, je reçois un code, une heure d'ouverture et une heure d'expiration à +15 min                                                                                    | Must     |
| EF2  | L'étudiant marque sa présence avec un code valide                          | Quand je saisis un code valide et non expiré, ma présence apparaît dans le tableau du formateur avec `source = ETUDIANT`                                                                   | Must     |
| EF3  | Le système refuse un code expiré ou une double présence                    | Un code expiré renvoie `410 CODE_EXPIRE` ; une deuxième tentative du même étudiant sur la même session renvoie `409 DEJA_PRESENT`                                                          | Must     |
| EF4  | Le système bloque temporairement après 5 échecs de code                    | Après 5 codes invalides consécutifs pour un même étudiant, toute nouvelle tentative est refusée pendant 2 minutes                                                                          | Should   |
| EF5  | Le formateur ajoute une présence manuellement                              | La présence ajoutée porte `source = FORMATEUR` et apparaît comme telle dans le tableau                                                                                                     | Must     |
| EF6  | L'étudiant dépose le lien de son exercice                                  | Un dépôt sur une session `OUVERTE` crée un exercice au statut `EN_ATTENTE_RELECTURE`                                                                                                       | Must     |
| EF7  | L'étudiant remplace le lien de son exercice avant le début d'une relecture | Tant qu'aucune relecture n'a démarré, un nouveau dépôt remplace l'ancien lien ; sinon `409 RELECTURE_DEJA_COMMENCEE`                                                                       | Must     |
| EF8  | Le système assigne un relecteur au hasard parmi les présents               | Le relecteur assigné n'est jamais l'auteur de l'exercice et figure dans la liste des présents à la session                                                                                 | Must     |
| EF9  | Le relecteur note et commente l'exercice assigné                           | Une relecture valide (note entière 0–20) passe l'exercice au statut `RELU` et verrouille définitivement la note                                                                            | Must     |
| EF10 | Le relecteur ne peut pas relire son propre exercice                        | Une tentative de relecture sur son propre exercice renvoie `403 AUTO_RELECTURE`                                                                                                            | Must     |
| EF11 | Une relecture déjà rendue ne peut pas être renvoyée                        | Une deuxième soumission sur la même relecture renvoie `409 RELECTURE_DEJA_RENDUE`                                                                                                          | Must     |
| EF12 | L'étudiant relu voit sa note et son commentaire, sans le nom du relecteur  | La réponse consultée par l'étudiant ne contient aucun champ identifiant le relecteur                                                                                                       | Must     |
| EF13 | Le formateur clôture une session manuellement                              | Un `PATCH` de clôture sur une session `OUVERTE` la fait passer à `CLOTUREE`                                                                                                                | Must     |
| EF14 | Une session se clôture automatiquement 24h après sa fin                    | Sans action du formateur, la session passe à `CLOTUREE` 24h après `expirationAt`                                                                                                           | Must     |
| EF15 | Le formateur rouvre une session clôturée par erreur                        | Un `PATCH` de réouverture sur une session `CLOTUREE` la fait repasser à `OUVERTE` ; le délai d'auto-clôture à 24h reste calculé depuis `expirationAt` d'origine, pas depuis la réouverture | Must     |
| EF16 | Le formateur consulte le tableau de suivi                                  | Le tableau affiche, par étudiant : présences, exercices déposés, moyenne des notes reçues, relectures encore en attente                                                                    | Must     |

## 5. Exigences non fonctionnelles

| Réf  | Exigence                  | Comment on la vérifie                                                                                      |
| ---- | ------------------------- | ---------------------------------------------------------------------------------------------------------- |
| ENF1 | Usage mobile              | Les trois écrans restent utilisables sur un écran de 375px de large sans défilement horizontal             |
| ENF2 | Temps de réponse          | Les endpoints du contrat répondent en moins d'1s avec un jeu de données de démonstration (< 100 étudiants) |
| ENF3 | Cohérence des données     | La moyenne affichée dans le tableau est calculée côté API, jamais recalculée côté frontend                 |
| ENF4 | Résilience au redémarrage | Le schéma de base est reconstruit automatiquement via migrations versionnées, sans intervention manuelle   |

## 6. Règles de gestion

| Réf  | Règle                                                                                                                              | Source                                                                 |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| RG1  | Le code de présence expire 15 minutes après l'ouverture de la session                                                              | Q2                                                                     |
| RG2  | Impossible de marquer sa présence après la fin de la session (code expiré)                                                         | Q3                                                                     |
| RG3  | Après 5 codes invalides, blocage de 2 minutes                                                                                      | Q4                                                                     |
| RG4  | Un étudiant ne peut jamais relire son propre exercice                                                                              | Q5                                                                     |
| RG5  | Un exercice a exactement un relecteur                                                                                              | Q6                                                                     |
| RG6  | Le relecteur est choisi au hasard parmi les étudiants présents à la session                                                        | Q7                                                                     |
| RG7  | L'étudiant relu voit note et commentaire, jamais l'identité du relecteur                                                           | Q8                                                                     |
| RG8  | La note est un entier compris entre 0 et 20                                                                                        | Q9                                                                     |
| RG9  | Une relecture rendue est définitive, aucune correction possible ensuite                                                            | Q15 (retenue contre Q10, voir la section 7 pour comprendre mon choix) |
| RG10 | Un exercice sans relecture rendue reste au statut `EN_ATTENTE_RELECTURE`, visible comme tel dans le tableau                        | Q11                                                                    |
| RG11 | Le dépôt d'exercice reste possible tant que la session n'est pas `CLOTUREE`                                                        | Q12                                                                    |
| RG12 | Le remplacement du lien d'exercice est possible tant qu'aucune relecture n'a démarré, indépendamment de la clôture de session      | Q13                                                                    |
| RG13 | Une présence ajoutée par le formateur est marquée `source = FORMATEUR`, distincte d'une présence saisie par l'étudiant             | Q14                                                                    |
| RG14 | Une session passe automatiquement à `CLOTUREE` 24h après `expirationAt` si le formateur n'a rien fait avant                        | Hypothèse (section 7)                                                  |
| RG15 | Le formateur peut clôturer ou rouvrir une session manuellement à tout moment ; une réouverture ne réinitialise pas le délai de 24h | Hypothèse (section 7)                                                  |

## 7. Zones d'ombre, hypothèses et contradictions tranchées

| Point                                                                                                      | Réponse client (Qx) ou hypothèse                                    | Décision retenue                                                                                                                           | Pourquoi                                                                                                                                                                                               |
| ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Modifiabilité de la note après envoi                                                                       | Q10 dit modifiable jusqu'à clôture ; Q15 dit définitive dès l'envoi | On retient **Q15** : la note est verrouillée dès que le relecteur l'envoie                                                                 | Q15 est postérieure dans l'échange et motivée par le client lui-même ("plus honnête pour tout le monde"), Q10 ne l'est pas. RG9 rend la clause de Q10 sans objet, ce qui est assumé ici explicitement. |
| Aucune opération de clôture de session dans le contrat imposé, alors que Q10/Q12/Q13/Q15 s'y réfèrent tous | Trou non couvert par les 16 questions                               | On ajoute au contrat deux opérations : clôture et réouverture manuelle de session, plus une clôture automatique à 24h après `expirationAt` | Sans cet endpoint, RG11 et RG14 sont invérifiables. Le délai de 24h protège le cas Q12 (étudiants sans connexion le soir même) tout en évitant qu'une session reste ouverte indéfiniment.              |
| Que se passe-t-il si le formateur clôture une session par erreur avant la fin du délai prévu ?             | Trou non couvert, soulevé pendant la conception                     | Réouverture manuelle possible à tout moment, sans réinitialiser le délai de 24h ni restreindre qui peut alors déposer                      | Évite un blocage définitif en cas d'erreur humaine, sans complexifier la règle par un ciblage étudiant que rien ne demande.                                                                            |
| Fenêtre de temps de l'ajout manuel de présence (EF5) : a-t-elle une limite comme le code ?                                                    | Trou non couvert par les 16 questions, soulevé pendant la conception     | Non : RG1 (15 min) ne s'applique qu'au code. L'ajout manuel passe par un endpoint dédié `POST /api/sessions/{id}/presences` et reste possible tant que la session est `OUVERTE`                                                                                       | Le formateur n'a pas de code à saisir ; la présence manuelle existe précisément pour rattraper ceux qui ont raté la fenêtre du code, lui appliquer la même limite la rendrait inutile.                                                                                      |
| Le `POST /api/exercices` renvoie `409 EXERCICE_DEJA_DEPOSE` dès le deuxième dépôt, ce qui contredit EF7/RG12 (remplacement possible tant qu'aucune relecture n'a démarré) | Contradiction interne au contrat imposé                             | Option (a) : remplacement via un endpoint dédié `PUT /api/exercices/{id}` (renvoie `409 RELECTURE_DEJA_COMMENCEE` si une relecture a démarré) ; le `409` du POST garde le sens « un exercice existe déjà pour ce couple session/étudiant, utilisez PUT »                | Préserve la sémantique REST (POST non idempotent, PUT pour le remplacement) sans modifier le comportement imposé du POST.                                                                                                                                                   |
| Que se passe-t-il si aucun relecteur éligible n'existe au moment du dépôt (un seul étudiant présent, RG4) ?                                                 | Trou non couvert, soulevé pendant la conception                     | Le dépôt est refusé avec `422 AUCUN_RELECTEUR_DISPONIBLE` ; l'étudiant peut réessayer dès qu'un autre présent est enregistré                                                                                                                                           | RG5 (Q6) impose exactement un relecteur par exercice : différer l'assignation créerait des exercices sans relecteur et violerait la règle imposée. Le refus est transitoire, pas définitif.                                                                                  |
| Quel refus renvoyer pendant le blocage de 2 minutes après 5 échecs de code (EF4/RG3) ?                                                                      | Trou non couvert                                                    | `429 TROP_ECHECS` sur `POST /api/presences`                                                                                                                                                                                                                          | EF4 est invérifiable sans code d'erreur dédié ; `429` est le statut standard du refus temporaire.                                                                                                                                                                           |
| Comment vérifier EF10 (`403 AUTO_RELECTURE`) alors que le `POST /api/relectures/{id}` imposé ne dit pas qui soumet ?                                        | Contradiction interne au contrat imposé                             | Le corps du POST exige aussi `relecteurId`                                                                                                                                                                                                                           | Sans émetteur identifié, le serveur ne peut pas détecter l'auto-relecture et le `403` imposé serait inopérant.                                                                                                                                                              |
| Que renvoie `POST /api/presences` quand le `etudiantId` n'existe pas ?                                                                           | Trou non couvert, soulevé pendant le développement EF2                  | `404 ETUDIANT_INCONNU` : le POST vérifie l'existence de l'étudiant avant tout                                                                                                                                            | Une présence rattachée à un étudiant fantôme fausserait le tableau (EF16) et violerait la contrainte d'intégrité. Cohérent avec `PROMOTION_INCONNUE` d'EF1.                                                                                                                                                            |
| Un code encore valide peut-il servir après la clôture de la session (l'étudiant pointe en retard) ?                                              | Trou non couvert, soulevé pendant le développement EF2                  | Non : `409 SESSION_CLOTUREE` quand la session est clôturée mais le code pas encore expiré ; si les deux s'appliquent, `410 CODE_EXPIRE` prime (cause racine vue par l'étudiant)                                                                                          | RG2 dit « impossible après la fin de la session » : la clôture est bien une fin. Le 410 prioritaire correspond à ce que l'étudiant peut comprendre : son code est trop vieux.                                                                                                                                          |
| Le code de présence est-il sensible à la casse et aux espaces ?                                                                                   | Trou non couvert, soulevé pendant le développement EF2                  | Non : le code est normalisé (trim + majuscules) avant la recherche                                                                                                                                          | Le code est tapé à la main sur mobile (ENF1) : refuser un étudiant légitime pour un espace accidentel ou une minuscule serait une erreur d'usage, pas de sécurité.                                                                                                                                                      |
| Que renvoie `POST /api/sessions` quand le `promotionId` n'existe pas ?                                                                        | Trou non couvert, soulevé pendant le développement EF1               | `404 PROMOTION_INCONNUE` : le POST vérifie l'existence de la promotion avant de créer la session                                                                                                                                                                       | Stocker une session rattachée à une promotion fantôme rendrait le tableau (EF16) incohérent : la session serait invisible alors qu'elle existe. Ajout additif au contrat, dans l'esprit du `PROMOTION_INCONNUE` déjà prévu sur `/api/tableau`.                                  |
| Que mesure exactement `relecturesEnAttente` dans le tableau (EF16) ?                                                                                        | Ambiguïté                                                           | Le nombre de relectures assignées à l'étudiant et pas encore rendues (son travail de relecteur en retard)                                                                                                                                                             | La colonne sert au formateur à repérer les relecteurs qui bloquent la chaîne ; les relectures reçues sur ses propres exercices se lisent via sa moyenne. Le relecteur consulte sa liste via `GET /api/relectures?relecteurId=`.                                              |

## 8. Contraintes techniques

Java 21+, Maven avec wrapper `mvnw` commité, Spring Boot en couches contrôleur/service/repository, DTO obligatoires (jamais d'entité JPA exposée directement — condition de RG7), validation des entrées et gestion centralisée des erreurs via `@RestControllerAdvice`, schéma versionné par migrations (Flyway ou Liquibase) avec des identifiants `bigint` auto-incrément générés par la base (`id BIGSERIAL PRIMARY KEY` sous PostgreSQL, équivalent `IDENTITY` sous H2 en test, jamais d'UUID applicatif, en cohérence avec les `integer/int64` du contrat), format d'erreur imposé `{ code, message }` sur toutes les erreurs. Frontend au choix (React, Angular, Next.js), appels API isolés dans une couche dédiée, aucun recalcul de règle métier côté client.

## 9. Livrables

Dépôt GitHub public `kfokam48-epreuve-225` avec `/docs`, `/api`, `/backend`, `/frontend` ; contrat d'API complété ; diagrammes versionnés en PlantUML (D1) et Mermaid (D2, D3, D4) ; backlog en issues ; `CHANGELOG.md` ; `README` d'installation testé depuis un clone vierge ; `JOURNAL.md` tenu à chaque étape.

## 10. Démarche prévue

1. Trancher les contradictions et le trou (fait ci-dessus), compléter le contrat d'API.
2. Rédiger les diagrammes D1/D2/D3/D4 en cohérence avec ce cahier des charges.
3. Créer le backlog en issues, priorité Must/Should/Could, renvoi aux EFx/RGx.
4. Poser `[JALON] analyse`, puis développer les stories Must uniquement pour `v0.1`.
5. Ouvrir l'enveloppe, absorber le changement, remettre ce document à jour dans un commit dédié.
6. Livrer `v1.0`, changelog, README testé, backlog restant trié.

Definition of Done : un ticket est terminé quand son critère d'acceptation est vérifié manuellement ou par un test, que le code est mergé dans `main` via PR liée à l'issue, et que l'issue est fermée.
