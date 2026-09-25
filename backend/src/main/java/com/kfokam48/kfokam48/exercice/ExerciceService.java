package com.kfokam48.kfokam48.exercice;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.relecture.RelectureEntity;
import com.kfokam48.kfokam48.relecture.RelectureRepository;
import com.kfokam48.kfokam48.session.EtudiantEntity;
import com.kfokam48.kfokam48.session.EtudiantInconnuException;
import com.kfokam48.kfokam48.session.EtudiantRepository;
import com.kfokam48.kfokam48.session.PresenceRepository;
import com.kfokam48.kfokam48.session.SessionClotureeException;
import com.kfokam48.kfokam48.session.SessionEntity;
import com.kfokam48.kfokam48.session.SessionInconnueException;
import com.kfokam48.kfokam48.session.SessionRepository;
import com.kfokam48.kfokam48.session.SessionStatut;

/**
 * EF6 : dépôt du lien d'exercice, avec assignation immédiate d'un relecteur
 * tiré au hasard parmi les présents hors auteur (EF8, RG4, RG5, RG6).
 * EF7 : remplacement du lien tant que la relecture n'est pas rendue (RG12).
 * EF12 : consultation de la note et du commentaire, sans le relecteur (RG7).
 */
@Service
public class ExerciceService {

    private static final int LONGUEUR_MAX_LIEN = 2048;

    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;
    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final Clock horloge;
    private final SecureRandom aleatoire = new SecureRandom();

    public ExerciceService(ExerciceRepository exercices, RelectureRepository relectures,
            SessionRepository sessions, EtudiantRepository etudiants, PresenceRepository presences,
            Clock horloge) {
        this.exercices = exercices;
        this.relectures = relectures;
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.horloge = horloge;
    }

    @Transactional
    public ExerciceEtat deposer(Long sessionId, Long etudiantId, String lienBrut) {
        // 1. Le lien est une URL http(s) exploitable (400)
        String lien = validerLien(lienBrut);

        // 2. La session existe (404)
        SessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionInconnueException(sessionId));

        // 3. L'auteur existe et appartient à la promotion de la session (404, section 7)
        EtudiantEntity auteur = etudiants.findById(etudiantId)
                .filter(e -> e.getPromotion().getId().equals(session.getPromotion().getId()))
                .orElseThrow(() -> new EtudiantInconnuException(etudiantId));

        // 4. Dépôt possible tant que la session n'est pas clôturée (409, RG11)
        if (session.getStatut() == SessionStatut.CLOTUREE) {
            throw new SessionClotureeException("La session est clôturée : il n'est plus possible de déposer un exercice.");
        }

        // 5. Un seul exercice par session, le remplacement passe par PUT (409)
        if (exercices.existsBySessionIdAndAuteurId(sessionId, etudiantId)) {
            throw new ExerciceDejaDeposeException();
        }

        // 6. Un relecteur éligible existe, sinon refus transitoire (422, section 7)
        List<Long> candidats = presences.presentsHorsAuteur(sessionId, etudiantId);
        if (candidats.isEmpty()) {
            throw new AucunRelecteurDisponibleException();
        }
        Long relecteurId = candidats.get(aleatoire.nextInt(candidats.size())); // RG6

        // 7. Cas nominal : exercice EN_ATTENTE_RELECTURE et sa relecture assignée (RG5)
        ExerciceEntity exercice = new ExerciceEntity();
        exercice.setSession(session);
        exercice.setAuteur(auteur);
        exercice.setLien(lien);
        exercice.setStatut(ExerciceStatut.EN_ATTENTE_RELECTURE);
        exercice.setDeposeAt(horloge.instant());
        exercices.save(exercice);

        RelectureEntity relecture = new RelectureEntity();
        relecture.setExercice(exercice);
        relecture.setRelecteur(etudiants.getReferenceById(relecteurId));
        relectures.save(relecture);

        return new ExerciceEtat(exercice.getId(), exercice.getStatut());
    }

    /**
     * EF7/RG12 : indépendant de la clôture de session ; seul compte le rendu de
     * la relecture. Le relecteur assigné reste le même.
     */
    @Transactional
    public ExerciceEtat remplacerLien(Long exerciceId, Long etudiantId, String lienBrut) {
        // 1. Le lien est une URL http(s) exploitable (400)
        String lien = validerLien(lienBrut);

        // 2. L'exercice existe (404)
        ExerciceEntity exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ExerciceInconnuException(exerciceId));

        // 3. Seul l'auteur remplace son lien (403, section 7)
        if (!exercice.getAuteur().getId().equals(etudiantId)) {
            throw new NonAuteurException();
        }

        // 4. Aucune relecture rendue (409, RG12 — « démarrée » tranché en section 7)
        if (exercice.getStatut() == ExerciceStatut.RELU) {
            throw new RelectureDejaCommenceeException();
        }

        exercice.setLien(lien);
        return new ExerciceEtat(exercice.getId(), exercice.getStatut());
    }

    /**
     * EF12/RG7 : l'étudiant consulte son exercice, note et commentaire inclus
     * une fois la relecture rendue. Rien ici ne lit le relecteur : le détail ne
     * peut donc pas le laisser fuir, même par erreur de sérialisation.
     */
    @Transactional(readOnly = true)
    public ExerciceDetail consulter(Long exerciceId) {
        ExerciceEntity exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ExerciceInconnuException(exerciceId));

        // Note et commentaire n'existent qu'une fois la relecture rendue (RG9)
        Optional<RelectureEntity> rendue = relectures.findByExerciceId(exerciceId)
                .filter(r -> r.getRendueAt() != null);

        return new ExerciceDetail(exercice.getId(), exercice.getLien(), exercice.getStatut(),
                rendue.map(RelectureEntity::getNote).orElse(null),
                rendue.map(RelectureEntity::getCommentaire).orElse(null));
    }

    /**
     * Le lien est ouvert par le relecteur : on n'accepte qu'une URL absolue
     * http(s) avec un hôte — ni texte libre, ni javascript:, ni file:.
     */
    static String validerLien(String lienBrut) {
        String lien = (lienBrut == null) ? "" : lienBrut.trim();
        if (lien.isEmpty() || lien.length() > LONGUEUR_MAX_LIEN) {
            throw new LienInvalideException();
        }
        try {
            URI uri = new URI(lien);
            String schema = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(schema.equals("http") || schema.equals("https")) || uri.getHost() == null) {
                throw new LienInvalideException();
            }
        } catch (URISyntaxException e) {
            throw new LienInvalideException();
        }
        return lien;
    }

    public record ExerciceEtat(Long id, ExerciceStatut statut) {
    }

    /** Aucun champ relecteur, par construction (RG7). */
    public record ExerciceDetail(Long id, String lien, ExerciceStatut statut, Integer note, String commentaire) {
    }
}
