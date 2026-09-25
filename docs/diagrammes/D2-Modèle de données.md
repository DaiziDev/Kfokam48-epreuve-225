# D2 — Modèle de données

```mermaid
erDiagram
    PROMOTION ||--o{ SESSION : contient
    SESSION ||--o{ PRESENCE : enregistre
    SESSION ||--o{ EXERCICE : recoit
    ETUDIANT ||--o{ PRESENCE : marque
    ETUDIANT ||--o{ EXERCICE : depose
    EXERCICE ||--o{ RELECTURE : "2 nouvelles, 1 possible héritée"
    ETUDIANT ||--o{ RELECTURE : effectue

    PROMOTION {
        bigint id PK "BIGSERIAL"
        string nom
    }

    SESSION {
        bigint id PK "BIGSERIAL"
        bigint promotionId FK
        string titre
        string code
        datetime ouvertureAt
        datetime expirationAt
        string statut "OUVERTE | CLOTUREE"
    }

    ETUDIANT {
        bigint id PK "BIGSERIAL"
        bigint promotionId FK
        string nom
    }

    PRESENCE {
        bigint id PK "BIGSERIAL"
        bigint sessionId FK
        bigint etudiantId FK
        string source "ETUDIANT | FORMATEUR"
        datetime marqueeAt
    }

    EXERCICE {
        bigint id PK "BIGSERIAL"
        bigint sessionId FK
        bigint etudiantId FK
        string lien
        string statut "EN_ATTENTE_RELECTURE | RELU"
        datetime deposeAt
    }

    RELECTURE {
        bigint id PK "BIGSERIAL"
        bigint exerciceId FK "unique avec relecteurId"
        bigint relecteurId FK
        int note "0 à 20, entier, nul tant que non rendue"
        string commentaire "nul tant que non rendue"
        datetime rendueAt "nul tant que non rendue"
    }
```

> Décision du ticket #23 : chaque nouveau dépôt crée deux lignes `RELECTURE`, assignées à deux étudiants
> présents différents. La contrainte unique porte sur le couple (`exerciceId`, `relecteurId`), et non
> sur le seul exercice. Chaque ligne garde sa note et son commentaire ; `rendueAt` verrouille ce rendu.
> La migration conserve les lignes historiques et ajoute une seconde affectation lorsqu'un pair
> présent éligible existe. Un exercice historique sans second candidat reste consultable avec sa note
> éventuellement provisoire.

## Identifiants : `bigint` auto-incrément, pas d'UUID

Toutes les clés primaires sont des `bigint` générés par la base (auto-incrément), en cohérence avec le contrat d'API (`api/contrat.yml`) qui impose des identifiants `integer/int64`.

Conséquence concrète sur les migrations Flyway :

- PostgreSQL : `id BIGSERIAL PRIMARY KEY` (ou `bigint GENERATED ALWAYS AS IDENTITY`), jamais `id UUID DEFAULT gen_random_uuid()`.
- Les clés étrangères référencent ces `bigint` : `promotionId BIGINT REFERENCES promotion(id)`, etc.
- H2 (profil de test) : `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, équivalent sémantique du `BIGSERIAL` PostgreSQL.

Le choix des valeurs d'identifiants appartient donc à la base, jamais au code applicatif : les entités JPA utilisent `GenerationType.IDENTITY` et ne génèrent aucun identifiant côté Java.
