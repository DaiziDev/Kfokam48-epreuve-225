package com.kfokam48.kfokam48.session;

public class PromotionInconnueException extends RuntimeException {

    private final Long promotionId;

    public PromotionInconnueException(Long promotionId) {
        super("La promotion " + promotionId + " n'existe pas.");
        this.promotionId = promotionId;
    }

    public Long getPromotionId() {
        return promotionId;
    }
}
