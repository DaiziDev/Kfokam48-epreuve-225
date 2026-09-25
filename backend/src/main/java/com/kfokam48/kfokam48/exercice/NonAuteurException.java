package com.kfokam48.kfokam48.exercice;

public class NonAuteurException extends RuntimeException {

    public NonAuteurException() {
        super("Seul l'auteur de l'exercice peut en remplacer le lien.");
    }
}
