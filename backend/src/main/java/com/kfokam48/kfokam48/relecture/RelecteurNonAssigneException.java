package com.kfokam48.kfokam48.relecture;

public class RelecteurNonAssigneException extends RuntimeException {

    public RelecteurNonAssigneException() {
        super("Cette relecture n'est pas assignée à cet étudiant.");
    }
}
