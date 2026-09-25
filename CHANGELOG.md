# CHANGELOG

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnement [Sémantique](https://semver.org/lang/fr/).

## [Non publié]

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
