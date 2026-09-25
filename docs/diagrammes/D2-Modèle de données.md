# D2 — Modèle de données

```mermaid
erDiagram
    PROMOTION ||--o{ SESSION : contient
    SESSION ||--o{ PRESENCE : enregistre
    SESSION ||--o{ EXERCICE : recoit
    ETUDIANT ||--o{ PRESENCE : marque
    ETUDIANT ||--o{ EXERCICE : depose
    EXERCICE ||--o| RELECTURE : "a au plus une"
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
    }

    RELECTURE {
        bigint id PK "BIGSERIAL"
        bigint exerciceId FK
        bigint relecteurId FK
        int note "0 à 20, entier"
        string commentaire
        datetime rendueAt
        boolean verrouillee
    }
```

## Identifiants : `bigint` auto-incrément, pas d'UUID

Toutes les clés primaires sont des `bigint` générés par la base (auto-incrément), en cohérence avec le contrat d'API (`api/contrat.yml`) qui impose des identifiants `integer/int64`.

Conséquence concrète sur les migrations Flyway :

- PostgreSQL : `id BIGSERIAL PRIMARY KEY` (ou `bigint GENERATED ALWAYS AS IDENTITY`), jamais `id UUID DEFAULT gen_random_uuid()`.
- Les clés étrangères référencent ces `bigint` : `promotionId BIGINT REFERENCES promotion(id)`, etc.
- H2 (profil de test) : `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, équivalent sémantique du `BIGSERIAL` PostgreSQL.

Le choix des valeurs d'identifiants appartient donc à la base, jamais au code applicatif : les entités JPA utilisent `GenerationType.IDENTITY` et ne génèrent aucun identifiant côté Java.