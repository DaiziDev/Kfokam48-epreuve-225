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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Relecture assignée au dépôt (EF8). Note, commentaire et rendueAt restent nuls
 * jusqu'au rendu (EF9) ; une relecture rendue est définitive (RG9).
 */
@Entity
@Table(name = "relecture")
public class RelectureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private ExerciceEntity exercice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private EtudiantEntity relecteur;

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
