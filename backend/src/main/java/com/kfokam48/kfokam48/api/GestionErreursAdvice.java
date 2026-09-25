package com.kfokam48.kfokam48.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kfokam48.kfokam48.session.CodeExpireException;
import com.kfokam48.kfokam48.session.CodeInconnuException;
import com.kfokam48.kfokam48.session.DejaPresentException;
import com.kfokam48.kfokam48.session.EtudiantInconnuException;
import com.kfokam48.kfokam48.session.PromotionInconnueException;
import com.kfokam48.kfokam48.session.SessionClotureeException;
import com.kfokam48.kfokam48.session.SessionDejaClotureeException;
import com.kfokam48.kfokam48.session.SessionDejaOuverteException;
import com.kfokam48.kfokam48.session.SessionInconnueException;

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

    /** Identifiant de session inexistant → 404 SESSION_INCONNUE (EF5, contrat). */
    @ExceptionHandler(SessionInconnueException.class)
    public ResponseEntity<CorpsErreur> sessionInconnue(SessionInconnueException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CorpsErreur("SESSION_INCONNUE", exception.getMessage()));
    }

    /** etudiantId inexistant → 404 ETUDIANT_INCONNU (décision section 7). */
    @ExceptionHandler(EtudiantInconnuException.class)
    public ResponseEntity<CorpsErreur> etudiantInconnu(EtudiantInconnuException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CorpsErreur("ETUDIANT_INCONNU", exception.getMessage()));
    }

    /** Code sans session → 400 CODE_INCONNU (EF3, D3). */
    @ExceptionHandler(CodeInconnuException.class)
    public ResponseEntity<CorpsErreur> codeInconnu(CodeInconnuException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CorpsErreur("CODE_INCONNU", exception.getMessage()));
    }

    /** Code dont la fenêtre de 15 min est passée → 410 CODE_EXPIRE (EF3, RG1). */
    @ExceptionHandler(CodeExpireException.class)
    public ResponseEntity<CorpsErreur> codeExpire(CodeExpireException exception) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new CorpsErreur("CODE_EXPIRE", exception.getMessage()));
    }

    /** Session clôturée mais code encore valide → 409 (RG2, décision section 7). */
    @ExceptionHandler(SessionClotureeException.class)
    public ResponseEntity<CorpsErreur> sessionCloturee(SessionClotureeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CorpsErreur("SESSION_CLOTUREE", exception.getMessage()));
    }

    /** Clôture d'une session déjà clôturée → 409 SESSION_DEJA_CLOTUREE (EF13). */
    @ExceptionHandler(SessionDejaClotureeException.class)
    public ResponseEntity<CorpsErreur> sessionDejaCloturee(SessionDejaClotureeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CorpsErreur("SESSION_DEJA_CLOTUREE", exception.getMessage()));
    }

    /** Réouverture d'une session déjà ouverte → 409 SESSION_DEJA_OUVERTE (EF15). */
    @ExceptionHandler(SessionDejaOuverteException.class)
    public ResponseEntity<CorpsErreur> sessionDejaOuverte(SessionDejaOuverteException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CorpsErreur("SESSION_DEJA_OUVERTE", exception.getMessage()));
    }

    /** Déjà pointé à cette session → 409 DEJA_PRESENT (EF3). */
    @ExceptionHandler(DejaPresentException.class)
    public ResponseEntity<CorpsErreur> dejaPresent(DejaPresentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CorpsErreur("DEJA_PRESENT", exception.getMessage()));
    }

    /** Filet : toute erreur non prévue reste au format { code, message }. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CorpsErreur> inattendue(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CorpsErreur("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
