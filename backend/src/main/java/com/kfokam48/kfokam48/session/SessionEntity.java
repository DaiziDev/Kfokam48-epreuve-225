package com.kfokam48.kfokam48.session;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_cours")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionEntity promotion;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 6, unique = true)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private Instant ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatut statut;

    public Long getId() {
        return id;
    }

    public PromotionEntity getPromotion() {
        return promotion;
    }

    public void setPromotion(PromotionEntity promotion) {
        this.promotion = promotion;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public void setOuvertureAt(Instant ouvertureAt) {
        this.ouvertureAt = ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public void setExpirationAt(Instant expirationAt) {
        this.expirationAt = expirationAt;
    }

    public SessionStatut getStatut() {
        return statut;
    }

    public void setStatut(SessionStatut statut) {
        this.statut = statut;
    }
}
