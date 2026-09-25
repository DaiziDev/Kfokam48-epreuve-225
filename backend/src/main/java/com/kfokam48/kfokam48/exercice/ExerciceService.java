package com.kfokam48.kfokam48.exercice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
 * EF6 : dépôt du lien d'exercice, avec assignation immédiate de deux relecteurs
 * distincts tirés au hasard parmi les présents hors auteur (EF8, RG4, RG5, RG6).
 * EF7 : remplacement du lien tant qu'aucune relecture n'est rendue (RG12).
 * EF12 : consultation des notes et commentaires, sans les relecteurs (RG7) —
 * moyenne définitive après deux rendus, note provisoire après un seul.
 */
@Service
public class ExerciceService {

    private static final int LONGUEUR_MAX_LIEN = 2048;
    /** Exigence révisée : chaque exercice est relu par deux relecteurs. */
    private static final int NOMBRE_RELECTEURS = 2;

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

        // 6. Deux relecteurs éligibles existent, sinon refus transitoire (422, section 7) :
        //    présents, distincts de l'auteur (RG4) et l'un de l'autre (RG5), tirés au hasard (RG6).
        List<Long> candidats = new ArrayList<>(presences.presentsHorsAuteur(sessionId, etudiantId));
        if (candidats.size() < NOMBRE_RELECTEURS) {
            throw new AucunRelecteurDisponibleException();
        }
        Set<Long> relecteurs = tirerDeuxRelecteurs(candidats);

        // 7. Cas nominal : exercice EN_ATTENTE_RELECTURE et ses deux relectures assignées (RG5)
        ExerciceEntity exercice = new ExerciceEntity();
        exercice.setSession(session);
        exercice.setAuteur(auteur);
        exercice.setLien(lien);
        exercice.setStatut(ExerciceStatut.EN_ATTENTE_RELECTURE);
        exercice.setDeposeAt(horloge.instant());
        exercices.save(exercice);

        var iterateurRelecteurs = relecteurs.iterator();
        for (int rang = RelectureEntity.PREMIERE; rang <= NOMBRE_RELECTEURS; rang++) {
            RelectureEntity relecture = new RelectureEntity();
            relecture.setExercice(exercice);
            relecture.setRelecteur(etudiants.getReferenceById(iterateurRelecteurs.next()));
            relecture.setRang(rang);
            relectures.save(relecture);
        }

        return new ExerciceEtat(exercice.getId(), exercice.getStatut());
    }

    /**
     * RG6 : tirage au hasard de deux relecteurs distincts parmi les candidats
     * éligibles. L'ensemble déduplique, puis on complète si le tirage est tombé
     * deux fois sur le même candidat.
     */
    private Set<Long> tirerDeuxRelecteurs(List<Long> candidats) {
        Set<Long> tires = new LinkedHashSet<>();
        while (tires.size() < NOMBRE_RELECTEURS) {
            tires.add(candidats.get(aleatoire.nextInt(candidats.size())));
        }
        return tires;
    }

    /**
     * EF7/RG12 : indépendant de la clôture de session ; seul compte le rendu de
     * la première relecture. Les relecteurs assignés restent les mêmes.
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

        // 4. Le lien se verrouille dès le premier rendu, même si la moyenne
        //    reste provisoire jusqu'au second (RG12).
        if (relectures.countByExerciceIdAndRendueAtIsNotNull(exerciceId) > 0) {
            throw new RelectureDejaCommenceeException();
        }

        exercice.setLien(lien);
        return new ExerciceEtat(exercice.getId(), exercice.getStatut());
    }

    /**
     * EF12/RG7 : l'étudiant consulte son exercice. Depuis l'exigence révisée,
     * la réponse porte les deux notes rendues, la moyenne (définitive après
     * deux rendus, provisoire après un seul) et les deux commentaires — mais
     * jamais l'identité d'un relecteur.
     */
    @Transactional(readOnly = true)
    public ExerciceDetail consulter(Long exerciceId) {
        ExerciceEntity exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ExerciceInconnuException(exerciceId));

        // Notes et commentaires n'existent qu'une fois les relectures rendues (RG9)
        List<RelectureEntity> relecturesExercice = relectures.findByExerciceIdOrderByRangAsc(exerciceId);
        List<Integer> notesRendues = relecturesExercice.stream()
                .filter(r -> r.getRendueAt() != null)
                .map(RelectureEntity::getNote)
                .toList();
        List<String> commentairesRendus = relecturesExercice.stream()
                .filter(r -> r.getRendueAt() != null)
                .map(RelectureEntity::getCommentaire)
                .toList();

        BigDecimal note = null;
        boolean provisoire = false;
        if (notesRendues.size() == 1) {
            // Un seul rendu : la note est provisoire, sans calcul (RG10)
            note = BigDecimal.valueOf(notesRendues.get(0));
            provisoire = true;
        } else if (notesRendues.size() >= 2) {
            // Deux rendus : la moyenne devient la note finale (exigence révisée).
            // Moyenne de deux entiers = X ou X.5 ; 15.0 est exposé comme 15,
            // et jamais en notation exponentielle par stripTrailingZeros.
            int somme = notesRendues.get(0) + notesRendues.get(1);
            BigDecimal moyenne = BigDecimal.valueOf(somme)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
            note = (moyenne.stripTrailingZeros().scale() <= 0)
                    ? BigDecimal.valueOf(moyenne.intValueExact())
                    : moyenne;
        }

        return new ExerciceDetail(exercice.getId(), exercice.getLien(), exercice.getStatut(),
                note, provisoire, commentairesRendus);
    }

    /** « Mes exercices » de l'étudiant — sans note ni relecteur : le détail passe par EF12. */
    @Transactional(readOnly = true)
    public List<ExerciceResume> listerParAuteur(Long etudiantId) {
        if (!etudiants.existsById(etudiantId)) {
            throw new EtudiantInconnuException(etudiantId);
        }
        return exercices.listerParAuteur(etudiantId).stream()
                .map(e -> new ExerciceResume(e.getId(), e.getSession().getId(), e.getSession().getTitre(),
                        e.getLien(), e.getStatut()))
                .toList();
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

    public record ExerciceResume(Long id, Long sessionId, String sessionTitre, String lien, ExerciceStatut statut) {
    }

    /**
     * Aucun champ relecteur, par construction (RG7). notes et commentaires
     * suivent le rang des relectures ; moyenne est nulle tant que les deux
     * notes ne sont pas rendues — après un seul rendu, noteProvisoire
     * l'expose (exigence révisée).
     */
    public record ExerciceDetail(Long id, String lien, ExerciceStatut statut,
            BigDecimal note, boolean noteProvisoire, List<String> commentaires) {
    }
}
