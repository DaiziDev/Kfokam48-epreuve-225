# D4 (Bonus) — États-transitions du cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE_RELECTURE : dépôt et assignation de deux pairs (EF6, EF8)

    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE : remplacement du lien tant qu'aucun pair n'a rendu (EF7, RG12)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE : premier rendu, note provisoire (EF9, RG10)
    EN_ATTENTE_RELECTURE --> RELU : second rendu, moyenne finale calculée (EF9, RG16)

    RELU --> [*] : deux rendus définitifs, aucune correction possible (RG9)

    note right of EN_ATTENTE_RELECTURE
        Un exercice reçoit deux affectations distinctes au dépôt.
        Après zéro rendu, aucune note n'est disponible.
        Après un rendu, sa note est affichée comme provisoire.
        Après deux rendus, leur moyenne est finale.
        Un exercice historique sans second candidat peut rester
        en attente avec une note provisoire.
    end note

    note right of RELU
        État terminal. Aucun retour possible :
        ni remplacement du lien, ni correction
        des notes ou commentaires (RG9).
    end note
```
