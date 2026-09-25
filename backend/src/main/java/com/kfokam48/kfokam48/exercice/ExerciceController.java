package com.kfokam48.kfokam48.exercice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService service;

    public ExerciceController(ExerciceService service) {
        this.service = service;
    }

    /** EF6/EF8 : dépôt du lien, relecteur assigné au hasard parmi les présents. */
    @PostMapping
    public ResponseEntity<ExerciceResponse> deposer(@Valid @RequestBody DeposerExerciceRequest requete) {
        ExerciceService.ExerciceEtat resultat = service.deposer(requete.sessionId(), requete.etudiantId(),
                requete.lien());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ExerciceResponse(resultat.id(), resultat.statut()));
    }

    /** EF7/RG12 : remplacement du lien par son auteur, tant que la relecture n'est pas rendue. */
    @PutMapping("/{id}")
    public ExerciceResponse remplacer(@PathVariable("id") Long exerciceId,
            @Valid @RequestBody RemplacerLienRequest requete) {
        ExerciceService.ExerciceEtat resultat = service.remplacerLien(exerciceId, requete.etudiantId(),
                requete.lien());
        return new ExerciceResponse(resultat.id(), resultat.statut());
    }

    /** « Mes exercices » : sans cette liste, l'étudiant perd l'id de son exercice après le dépôt (section 7). */
    @GetMapping
    public List<ExerciceResumeResponse> lister(@RequestParam("etudiantId") Long etudiantId) {
        return service.listerParAuteur(etudiantId).stream()
                .map(e -> new ExerciceResumeResponse(e.id(), e.sessionId(), e.sessionTitre(), e.lien(), e.statut()))
                .toList();
    }

    /** EF12/RG7 : détail de l'exercice, note et commentaire inclus, jamais le relecteur. */
    @GetMapping("/{id}")
    public ExerciceDetailResponse consulter(@PathVariable("id") Long exerciceId) {
        ExerciceService.ExerciceDetail detail = service.consulter(exerciceId);
        return new ExerciceDetailResponse(detail.id(), detail.lien(), detail.statut(), detail.note(),
                detail.commentaire());
    }

    /** Corps du contrat : { sessionId, etudiantId, lien }, tous obligatoires. */
    public record DeposerExerciceRequest(
            @NotNull(message = "Le sessionId est obligatoire.") Long sessionId,
            @NotNull(message = "L'etudiantId est obligatoire.") Long etudiantId,
            @NotBlank(message = "Le lien est obligatoire.") String lien) {
    }

    /** Corps du contrat : { etudiantId, lien }, les deux obligatoires. */
    public record RemplacerLienRequest(
            @NotNull(message = "L'etudiantId est obligatoire.") Long etudiantId,
            @NotBlank(message = "Le lien est obligatoire.") String lien) {
    }

    /** Réponse 201/200 strictement conforme au contrat : { id, statut }, aucun champ relecteur. */
    public record ExerciceResponse(Long id, ExerciceStatut statut) {
    }

    /** Ligne de « mes exercices » : aucun champ relecteur (RG7), note via GET /{id}. */
    public record ExerciceResumeResponse(Long id, Long sessionId, String sessionTitre, String lien,
            ExerciceStatut statut) {
    }

    /**
     * Réponse du contrat : { id, lien, statut, note, commentaire }, note et
     * commentaire nuls tant que la relecture n'est pas rendue. C'est ce DTO,
     * et non l'entité, qui garantit RG7 : il n'a aucun champ relecteur.
     */
    public record ExerciceDetailResponse(Long id, String lien, ExerciceStatut statut, Integer note,
            String commentaire) {
    }
}
