package com.kfokam48.kfokam48.relecture;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.kfokam48.exercice.ExerciceEntity;
import com.kfokam48.kfokam48.exercice.ExerciceStatut;
import com.kfokam48.kfokam48.session.EtudiantInconnuException;
import com.kfokam48.kfokam48.session.EtudiantRepository;

/**
 * EF9 : le relecteur assigné rend note et commentaire, l'exercice passe à RELU.
 * EF10/RG4 : jamais sur son propre exercice. EF11/RG9 : un seul rendu, définitif.
 */
@Service
public class RelectureService {

    private static final BigDecimal NOTE_MIN = BigDecimal.ZERO;
    private static final BigDecimal NOTE_MAX = BigDecimal.valueOf(20);
    private static final int LONGUEUR_MAX_COMMENTAIRE = 4000;

    private final RelectureRepository relectures;
    private final EtudiantRepository etudiants;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures, EtudiantRepository etudiants, Clock horloge) {
        this.relectures = relectures;
        this.etudiants = etudiants;
        this.horloge = horloge;
    }

    @Transactional
    public RelectureRendue rendre(Long relectureId, Long relecteurId, BigDecimal noteBrute, String commentaireBrut) {
        // 1. Note entière 0–20 et commentaire exploitable (400, RG8)
        int note = validerNote(noteBrute);
        String commentaire = commentaireBrut.trim();
        if (commentaire.length() > LONGUEUR_MAX_COMMENTAIRE) {
            throw new CommentaireInvalideException();
        }

        // 2. La relecture existe (404) — verrouillée jusqu'à la fin de la transaction
        RelectureEntity relecture = relectures.verrouillerParId(relectureId)
                .orElseThrow(() -> new RelectureInconnueException(relectureId));
        ExerciceEntity exercice = relecture.getExercice();

        // 3. Jamais sur son propre exercice (403, EF10/RG4)
        if (exercice.getAuteur().getId().equals(relecteurId)) {
            throw new AutoRelectureException();
        }

        // 4. Seul le relecteur assigné rend la relecture (403, section 7)
        if (!relecture.getRelecteur().getId().equals(relecteurId)) {
            throw new RelecteurNonAssigneException();
        }

        // 5. Une relecture rendue est définitive (409, EF11/RG9)
        if (relecture.getRendueAt() != null) {
            throw new RelectureDejaRendueException();
        }

        // 6. Cas nominal : note verrouillée, exercice RELU (EF9)
        Instant maintenant = horloge.instant();
        relecture.setNote(note);
        relecture.setCommentaire(commentaire);
        relecture.setRendueAt(maintenant);
        exercice.setStatut(ExerciceStatut.RELU);

        return new RelectureRendue(relecture.getId(), exercice.getId(), note, commentaire, maintenant);
    }

    /** EF9 : relectures assignées au relecteur, à faire ou déjà rendues. */
    @Transactional(readOnly = true)
    public List<RelectureAssignee> listerParRelecteur(Long relecteurId) {
        if (!etudiants.existsById(relecteurId)) {
            throw new EtudiantInconnuException(relecteurId);
        }
        return relectures.listerParRelecteur(relecteurId).stream()
                .map(r -> new RelectureAssignee(r.getId(), r.getExercice().getId(), r.getExercice().getLien(),
                        r.getRendueAt() != null))
                .toList();
    }

    /**
     * RG8 : la note arrive en BigDecimal pour voir la valeur exacte envoyée —
     * un Integer laisserait Jackson tronquer 12.5 en 12 sans erreur.
     * 12.0 est accepté (valeur entière), 12.5 refusé.
     */
    static int validerNote(BigDecimal note) {
        if (note == null) {
            throw new NoteInvalideException();
        }
        BigDecimal normalisee = note.stripTrailingZeros();
        if (normalisee.scale() > 0 || normalisee.compareTo(NOTE_MIN) < 0 || normalisee.compareTo(NOTE_MAX) > 0) {
            throw new NoteInvalideException();
        }
        return normalisee.intValueExact();
    }

    public record RelectureRendue(Long id, Long exerciceId, int note, String commentaire, Instant rendueAt) {
    }

    public record RelectureAssignee(Long id, Long exerciceId, String exerciceLien, boolean rendue) {
    }
}
