-- Deux relectures par exercice (exigence révisée) : chaque exercice est attribué
-- à deux relecteurs différents de son auteur et l'un de l'autre (RG4, RG5).
--
-- Les données existantes sont préservées :
--   * les exercices restent en l'état, aucune suppression ni re-création ;
--   * les relectures déjà rendues gardent leur note, commentaire et rendue_at ;
--   * la relecture déjà en place devient la première des deux (rang 1) ;
--   * un second relecteur est assigné aux exercices qui n'en ont qu'un, pris
--     parmi les présents de la session hors auteur et hors premier relecteur,
--     le plus ancien d'abord — même règle d'éligibilité qu'au dépôt (RG4, RG6).
--     Sans second candidat éligible, l'exercice conserve une seule relecture :
--     son état historique reste lisible (voir V7 : la note provisoire et la
--     moyenne restent calculées sur les relectures rendues).

-- 1. L'unicité « exactement un relecteur par exercice » (RG5 version 1) cède la
--    place à « un relecteur ne relit qu'une fois le même exercice ».
ALTER TABLE relecture DROP CONSTRAINT uq_relecture_exercice;
ALTER TABLE relecture
    ADD CONSTRAINT uq_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- 2. Toute relecture encore assignée reçoit un rang : la plus ancienne (ordre
--    de création historique, repris par l'identité) devient la première de son
--    exercice. La colonne rang distingue désormais la première et la seconde
--    relecture (1 = première, 2 = seconde).
ALTER TABLE relecture ADD COLUMN rang INTEGER;
UPDATE relecture r
SET rang = (
    SELECT COUNT(*) + 1
    FROM relecture anterieure
    WHERE anterieure.exercice_id = r.exercice_id
      AND anterieure.id < r.id
);
ALTER TABLE relecture ALTER COLUMN rang SET NOT NULL;
ALTER TABLE relecture
    ADD CONSTRAINT uq_relecture_exercice_rang UNIQUE (exercice_id, rang);
ALTER TABLE relecture
    ADD CONSTRAINT ck_relecture_rang CHECK (rang IN (1, 2));

-- 3. Un second relecteur est assigné quand il existe un candidat éligible :
--    présent à la session de l'exercice, autre que l'auteur et que le premier
--    relecteur. Le plus ancien présent est retenu pour rester déterministe —
--    le dépôt (EF6) tire désormais au hasard deux relecteurs distincts.
INSERT INTO relecture (exercice_id, relecteur_id, rang)
SELECT r.exercice_id, MIN(p.etudiant_id), 2
FROM relecture r
JOIN exercice x ON x.id = r.exercice_id
JOIN presence p ON p.session_id = x.session_id
WHERE r.rang = 1
  AND NOT EXISTS (
      SELECT 1 FROM relecture seconde
      WHERE seconde.exercice_id = r.exercice_id AND seconde.rang = 2)
  AND p.etudiant_id <> x.etudiant_id
  AND p.etudiant_id <> r.relecteur_id
GROUP BY r.exercice_id;
