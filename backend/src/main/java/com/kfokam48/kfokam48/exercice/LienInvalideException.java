package com.kfokam48.kfokam48.exercice;

public class LienInvalideException extends RuntimeException {

    public LienInvalideException() {
        super("Le lien doit être une URL http ou https complète (2048 caractères maximum).");
    }
}
