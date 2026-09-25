package com.kfokam48.kfokam48.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier EF2/EF3 : l'étudiant marque sa présence avec le code éphémère.
 * EF5/RG13 : le formateur ajoute une présence manuelle, marquée source = FORMATEUR.
 * L'ordre des vérifications suit le diagramme de séquence D3, avec une règle de
 * priorité tranchée en section 7 : un code expiré prime sur une session clôturée
 * (c'est la cause racine vue par l'étudiant).
 *
 * RG3 (5 échecs → blocage 2 min) se branchera ici : point d'entrée unique.
 */
@Service
public class PresenceService {

    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final Clock horloge;

    public PresenceService(SessionRepository sessions, EtudiantRepository etudiants,
            PresenceRepository presences, Clock horloge) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.horloge = horloge;
    }

    @Transactional
    public PresenceCreee enregistrer(String codeBrut, Long etudiantId) {
        // 0. L'étudiant doit exister (404, décision section 7)
        EtudiantEntity etudiant = etudiants.findById(etudiantId)
                .orElseThrow(() -> new EtudiantInconnuException(etudiantId));

        // 1. Le code mène à une session (400) — normalisation : saisie manuelle
        String code = normaliser(codeBrut);
        SessionEntity session = sessions.findByCode(code)
                .orElseThrow(() -> new CodeInconnuException(code));

        // 2. Le code n'est pas expiré (410, RG1) — prime sur la clôture
        if (horloge.instant().isAfter(session.getExpirationAt())) {
            throw new CodeExpireException();
        }

        // 3. La session est encore ouverte (409, RG2 au sens état) — tranché section 7
        if (session.getStatut() == SessionStatut.CLOTUREE) {
            throw new SessionClotureeException();
        }

        // 4. Pas de double présence (409, EF3)
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new DejaPresentException();
        }

        // 5. Cas nominal : présence créée avec source = ETUDIANT (EF2)
        return creer(session, etudiant, PresenceSource.ETUDIANT);
    }

    /**
     * EF5/RG13 : le formateur ajoute la présence d'un étudiant qui n'a pas pu
     * saisir le code. Pas de contrôle d'expiration : RG1 ne vise que le code
     * (section 7) — seul l'état OUVERTE de la session compte.
     */
    @Transactional
    public PresenceCreee ajouterParFormateur(Long sessionId, Long etudiantId) {
        // 1. La session existe (404)
        SessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionInconnueException(sessionId));

        // 2. L'étudiant existe et appartient à la promotion de la session (404, section 7)
        EtudiantEntity etudiant = etudiants.findById(etudiantId)
                .filter(e -> e.getPromotion().getId().equals(session.getPromotion().getId()))
                .orElseThrow(() -> new EtudiantInconnuException(etudiantId));

        // 3. La session est encore ouverte (409)
        if (session.getStatut() == SessionStatut.CLOTUREE) {
            throw new SessionClotureeException();
        }

        // 4. Pas de double présence, quelle que soit la source de la première (409)
        if (presences.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new DejaPresentException();
        }

        // 5. Cas nominal : présence créée avec source = FORMATEUR (RG13)
        return creer(session, etudiant, PresenceSource.FORMATEUR);
    }

    /** EF5 : les présents d'une session, chacun avec sa source pour les distinguer. */
    @Transactional(readOnly = true)
    public List<PresenceListee> listerParSession(Long sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw new SessionInconnueException(sessionId);
        }
        return presences.listerParSession(sessionId).stream()
                .map(p -> new PresenceListee(p.getId(), p.getEtudiant().getId(), p.getEtudiant().getNom(),
                        p.getSource(), p.getMarqueeAt()))
                .toList();
    }

    private PresenceCreee creer(SessionEntity session, EtudiantEntity etudiant, PresenceSource source) {
        PresenceEntity presence = new PresenceEntity();
        presence.setSession(session);
        presence.setEtudiant(etudiant);
        presence.setSource(source);
        presence.setMarqueeAt(horloge.instant());
        presences.save(presence);

        return new PresenceCreee(presence.getId(), session.getId(), etudiant.getId(), presence.getSource());
    }

    /**
     * Le code est tapé à la main sur mobile (ENF1) : espaces et casse ne doivent
     * pas faire échouer un étudiant légitime.
     */
    private static String normaliser(String codeBrut) {
        String code = (codeBrut == null) ? "" : codeBrut.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new CodeInconnuException("");
        }
        return code;
    }

    public record PresenceCreee(Long id, Long sessionId, Long etudiantId, PresenceSource source) {
    }

    public record PresenceListee(Long id, Long etudiantId, String nom, PresenceSource source,
            Instant marqueeAt) {
    }
}
