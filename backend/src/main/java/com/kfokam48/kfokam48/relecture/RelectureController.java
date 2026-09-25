package com.kfokam48.kfokam48.relecture;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService service;

    public RelectureController(RelectureService service) {
        this.service = service;
    }

    /** EF9/EF10/EF11 : le relecteur assigné rend note et commentaire. */
    @PostMapping("/{id}")
    public RelectureRendueResponse rendre(@PathVariable("id") Long relectureId,
            @Valid @RequestBody RendreRelectureRequest requete) {
        RelectureService.RelectureRendue resultat = service.rendre(relectureId, requete.relecteurId(),
                requete.note(), requete.commentaire());
        return new RelectureRendueResponse(resultat.id(), resultat.exerciceId(), resultat.note(),
                resultat.commentaire(), resultat.rendueAt());
    }

    /** EF9 : relectures assignées à un relecteur — sans elle, il ignore ce qu'il doit relire. */
    @GetMapping
    public List<RelectureAssigneeResponse> lister(@RequestParam("relecteurId") Long relecteurId) {
        return service.listerParRelecteur(relecteurId).stream()
                .map(r -> new RelectureAssigneeResponse(r.id(), r.exerciceId(), r.exerciceLien(), r.rendue()))
                .toList();
    }

    /** Corps du contrat : { relecteurId, note, commentaire }, tous obligatoires. */
    public record RendreRelectureRequest(
            @NotNull(message = "Le relecteurId est obligatoire.") Long relecteurId,
            @NotNull(message = "La note est obligatoire.") BigDecimal note,
            @NotBlank(message = "Le commentaire est obligatoire.") String commentaire) {
    }

    /** Réponse 200 : la relecture telle qu'enregistrée, désormais verrouillée. */
    public record RelectureRendueResponse(Long id, Long exerciceId, int note, String commentaire,
            Instant rendueAt) {
    }

    /** Ligne du contrat : { id, exerciceId, exerciceLien, rendue }. */
    public record RelectureAssigneeResponse(Long id, Long exerciceId, String exerciceLien, boolean rendue) {
    }
}
