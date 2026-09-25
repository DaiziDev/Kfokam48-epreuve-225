package com.kfokam48.kfokam48.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kfokam48.kfokam48.session.PromotionInconnueException;

/**
 * Gestion centralisée des erreurs : TOUTES les réponses d'erreur respectent le
 * format imposé { code, message }. Aucune stack trace, jamais la page d'erreur
 * par défaut de Spring (le contrat vaut zéro sur ce critère).
 */
@RestControllerAdvice
public class GestionErreursAdvice {

    public record CorpsErreur(String code, String message) {
    }

    /** Champ manquant ou vide sur un DTO validé → 400 (EF1). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CorpsErreur> champsRequis(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getDefaultMessage())
                .collect(Collectors.toList());
        String message = String.join(" ", messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CorpsErreur("CHAMPS_REQUIS", message));
    }

    /** Corps de requête illisible (JSON malformé, encodage invalide) → 400, jamais 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CorpsErreur> corpsIllisible(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CorpsErreur("CORPS_INVALIDE", "Le corps de la requête est illisible ou le JSON est invalide."));
    }

    /** promotionId inexistant → 404 PROMOTION_INCONNUE (décision section 7). */
    @ExceptionHandler(PromotionInconnueException.class)
    public ResponseEntity<CorpsErreur> promotionInconnue(PromotionInconnueException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CorpsErreur("PROMOTION_INCONNUE", exception.getMessage()));
    }

    /** Filet : toute erreur non prévue reste au format { code, message }. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CorpsErreur> inattendue(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CorpsErreur("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
