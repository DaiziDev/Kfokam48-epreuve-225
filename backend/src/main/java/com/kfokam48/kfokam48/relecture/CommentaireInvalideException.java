package com.kfokam48.kfokam48.relecture;

public class CommentaireInvalideException extends RuntimeException {

    public CommentaireInvalideException() {
        super("Le commentaire ne peut pas dépasser 4000 caractères.");
    }
}
