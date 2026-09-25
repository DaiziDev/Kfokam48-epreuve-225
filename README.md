# KFOKAM48 — Présence & Relecture

Application de gestion pour la formation KFOKAM48 : prise de présence par code temporaire, dépôt d'exercices en ligne, relecture attribuée aléatoirement entre pairs présents, et tableau de suivi pour le formateur.

## Structure du dépôt

| Dossier   | Contenu                                                       |
| --------- | ------------------------------------------------------------- |
| `docs/`   | Cahier des charges et diagrammes (D1–D4)                      |
| `api/`    | Contrat d'API OpenAPI (`contrat.yml`)                         |
| `backend/`| API Spring Boot (Java 21, Maven, JPA, Flyway, PostgreSQL/H2)  |
| `frontend/`| Application Angular (SSR, Tailwind CSS)                      |

## Prérequis

- Java 21+
- Docker (pour PostgreSQL) — ou une instance PostgreSQL existante
- Node.js 20+ et npm (frontend)

## Installation depuis un clone vierge

### 1. Base de données

```bash
docker run -d --name kfokam48-db \
  -e POSTGRES_USER=kfokam48 \
  -e POSTGRES_PASSWORD=kfokam48 \
  -e POSTGRES_DB=kfokam48 \
  -p 5432:5432 postgres:16
```

Le schéma est créé automatiquement au démarrage par les migrations Flyway versionnées — aucune intervention manuelle.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

L'API démarre sur http://localhost:8080. La console H2 n'est active qu'en profil de test.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

L'application démarre sur http://localhost:4200.

## Vérifier l'installation

```bash
curl http://localhost:8080/api/tableau?promotionId=1
# → 200 avec un tableau vide, ou 404 { "code": "PROMOTION_INCONNUE", ... } si la promotion n'existe pas
```

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

## Documentation

- `docs/CAHIER_DES_CHARGES.md` — exigences (EF/RG), règles tranchées, contraintes techniques
- `api/contrat.yml` — les 11 opérations de l'API et le format d'erreur imposé
- `docs/diagrammes/` — D1 cas d'utilisation (PlantUML), D2 modèle de données, D3 séquence, D4 états-transitions (Mermaid)
- `JOURNAL.md` — journal de bord tenu à chaque étape
- `CHANGELOG.md` — historique des versions
