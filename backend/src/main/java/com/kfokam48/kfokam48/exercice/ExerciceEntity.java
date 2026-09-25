package com.kfokam48.kfokam48.exercice;

import java.time.Instant;

import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.SessionEntity;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "exercice", uniqueConstraints = @UniqueConstraint(
        name = "uq_exercice_session_etudiant", columnNames = { "session_id", "etudiant_id" }))
public class ExerciceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private EtudiantEntity auteur;

    @Column(nullable = false, length = 2048)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExerciceStatut statut;

    @Column(name = "depose_at", nullable = false)
    private Instant deposeAt;

    public Long getId() {
        return id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public void setSession(SessionEntity session) {
        this.session = session;
    }

    public EtudiantEntity getAuteur() {
        return auteur;
    }

    public void setAuteur(EtudiantEntity auteur) {
        this.auteur = auteur;
    }

    public String getLien() {
        return lien;
    }

    public void setLien(String lien) {
        this.lien = lien;
    }

    public ExerciceStatut getStatut() {
        return statut;
    }

    public void setStatut(ExerciceStatut statut) {
        this.statut = statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }

    public void setDeposeAt(Instant deposeAt) {
        this.deposeAt = deposeAt;
    }
}
