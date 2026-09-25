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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "presence", uniqueConstraints = @UniqueConstraint(
        name = "uq_presence_session_etudiant", columnNames = { "session_id", "etudiant_id" }))
public class PresenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private EtudiantEntity etudiant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PresenceSource source;

    @Column(name = "marquee_at", nullable = false)
    private Instant marqueeAt;

    public Long getId() {
        return id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public void setSession(SessionEntity session) {
        this.session = session;
    }

    public EtudiantEntity getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(EtudiantEntity etudiant) {
        this.etudiant = etudiant;
    }

    public PresenceSource getSource() {
        return source;
    }

    public void setSource(PresenceSource source) {
        this.source = source;
    }

    public Instant getMarqueeAt() {
        return marqueeAt;
    }

    public void setMarqueeAt(Instant marqueeAt) {
        this.marqueeAt = marqueeAt;
    }
}
