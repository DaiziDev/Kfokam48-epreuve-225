# KFOKAM48 — Présence & Relecture

Application de gestion pour la formation KFOKAM48 : prise de présence par code temporaire, dépôt d'exercices en ligne, relecture attribuée aléatoirement entre pairs présents, et tableau de suivi pour le formateur.

## Structure du dépôt

| Dossier   | Contenu                                                       |
| --------- | ------------------------------------------------------------- |
| `docs/`   | Cahier des charges et diagrammes (D1–D4)                      |
| `api/`    | Contrat d'API OpenAPI (`contrat.yml`)                         |
| `backend/`| API Spring Boot (Java 21, Maven, JPA, Flyway, PostgreSQL/H2)  |
| `frontend/`| Application Angular (SPA, Tailwind CSS)                      |

## Démarrage rapide avec Docker

Seul prérequis : Docker (Docker Desktop sous Windows et macOS).

```bash
docker compose up --build
```

Puis ouvrir **http://localhost:8090**. Trois conteneurs démarrent dans l'ordre :

| Service    | Rôle                                                               | Exposé              |
| ---------- | ------------------------------------------------------------------ | ------------------- |
| `db`       | PostgreSQL 17, données dans le volume `db-data`                    | non                 |
| `backend`  | API Spring Boot ; migrations Flyway au démarrage (schéma + démo)   | non                 |
| `frontend` | nginx : sert l'application et relaie `/api` vers le backend        | `8090` → `80`       |

- Swagger UI : http://localhost:8090/swagger-ui.html
- Changer de port : `WEB_PORT=9000 docker compose up --build`
- Identifiants de base : `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (défaut `kfokam48`), surchargeables par variables d'environnement ou fichier `.env`
- Arrêter : `docker compose down` — repartir d'une base vide : `docker compose down -v`

## Installation manuelle (développement)

### Prérequis

- Java 21+
- Docker (pour PostgreSQL) — ou une instance PostgreSQL existante
- Node.js 20.19+ et npm (frontend)

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

L'API démarre sur http://localhost:8081. La console H2 n'est active qu'en profil de test.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

L'application démarre sur http://localhost:4200 (le proxy de développement relaie `/api` vers le port 8081).

## Vérifier l'installation

```bash
# Docker
curl "http://localhost:8090/api/etudiants?promotionId=1"
# Installation manuelle
curl "http://localhost:8081/api/etudiants?promotionId=1"
# → 200 avec les trois étudiants de démonstration de la promotion KFOKAM48
```

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

## Documentation

- `docs/CAHIER_DES_CHARGES.md` — exigences (EF/RG), règles tranchées, contraintes techniques
- `api/contrat.yml` — les opérations de l'API et le format d'erreur imposé
- `docs/diagrammes/` — D1 cas d'utilisation (PlantUML), D2 modèle de données, D3 séquence, D4 états-transitions (Mermaid)
- `docs/JOURNAL.md` — journal de bord tenu à chaque étape
- `docs/ARCHITECTURE.md` — décisions d'architecture backend et frontend
- `CHANGELOG.md` — historique des versions
