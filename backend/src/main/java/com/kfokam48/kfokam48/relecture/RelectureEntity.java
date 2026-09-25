package com.kfokam48.kfokam48.relecture;

import java.time.Instant;

import com.kfokam48.kfokam48.exercice.ExerciceEntity;
import com.kfokam48.kfokam48.session.EtudiantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Relecture assignée au dépôt — deux par exercice depuis l'exigence révisée
 * (ticket #23), distinctes par le rang : 1 = première, 2 = seconde. Note,
 * commentaire et rendueAt restent nuls jusqu'au rendu (EF9) ; une relecture
 * rendue est définitive (RG9). Un même relecteur ne relit qu'une fois le même
 * exercice (uq_relecture_exercice_relecteur).
 */
@Entity
@Table(name = "relecture", uniqueConstraints = {
        @UniqueConstraint(name = "uq_relecture_exercice_relecteur", columnNames = { "exercice_id", "relecteur_id" }),
        @UniqueConstraint(name = "uq_relecture_exercice_rang", columnNames = { "exercice_id", "rang" }) })
public class RelectureEntity {

    /** Première relecture de l'exercice (celle de l'ancien modèle, préservée). */
    public static final int PREMIERE = 1;
    /** Seconde relecture de l'exercice (ajoutée par l'exigence révisée). */
    public static final int SECONDE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false)
    private ExerciceEntity exercice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private EtudiantEntity relecteur;

    /** Position de la relecture sur l'exercice : 1 (première) ou 2 (seconde). */
    @Column(nullable = false)
    private int rang;

    private Integer note;

    @Column(length = 4000)
    private String commentaire;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    public Long getId() {
        return id;
    }

    public ExerciceEntity getExercice() {
        return exercice;
    }

    public void setExercice(ExerciceEntity exercice) {
        this.exercice = exercice;
    }

    public EtudiantEntity getRelecteur() {
        return relecteur;
    }

    public void setRelecteur(EtudiantEntity relecteur) {
        this.relecteur = relecteur;
    }

    public int getRang() {
        return rang;
    }

    public void setRang(int rang) {
        this.rang = rang;
    }

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }

    public void setRendueAt(Instant rendueAt) {
        this.rendueAt = rendueAt;
    }
}
