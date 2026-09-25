-- Nettoie les données créées pendant les essais, en conservant la promotion
-- KFOKAM48 et ses étudiants de démonstration.
-- À lancer sur la base locale kfokam48, après avoir arrêté le backend.

BEGIN;

TRUNCATE TABLE relecture, exercice, presence, session_cours RESTART IDENTITY;

COMMIT;

SELECT
    (SELECT count(*) FROM session_cours) AS sessions,
    (SELECT count(*) FROM presence) AS presences,
    (SELECT count(*) FROM exercice) AS exercices,
    (SELECT count(*) FROM relecture) AS relectures,
    (SELECT count(*) FROM etudiant) AS etudiants_conserves,
    (SELECT count(*) FROM promotion) AS promotions_conservees;
