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

**Fait :**

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
