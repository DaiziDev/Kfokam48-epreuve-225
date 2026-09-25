package com.kfokam48.kfokam48.exercice;

public class RelectureDejaCommenceeException extends RuntimeException {

    public RelectureDejaCommenceeException() {
        super("La relecture de cet exercice a déjà été rendue : le lien ne peut plus être remplacé.");
    }
}
