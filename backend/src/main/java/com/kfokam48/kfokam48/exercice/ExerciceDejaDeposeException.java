package com.kfokam48.kfokam48.exercice;

public class ExerciceDejaDeposeException extends RuntimeException {

    public ExerciceDejaDeposeException() {
        super("Un exercice est déjà déposé pour cette session : utilisez PUT /api/exercices/{id} pour remplacer le lien.");
    }
}
