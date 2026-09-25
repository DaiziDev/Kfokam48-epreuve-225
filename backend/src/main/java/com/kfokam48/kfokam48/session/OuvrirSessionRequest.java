package com.kfokam48.kfokam48.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de POST /api/sessions. La validation à la frontière porte les deux
 * champs requis par le contrat — un champ manquant doit donner 400 (EF1).
 */
public class OuvrirSessionRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    private String titre;

    @NotNull(message = "Le promotionId est obligatoire.")
    private Long promotionId;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }
}
