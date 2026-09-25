package com.kfokam48.kfokam48.exercice;

public class ExerciceInconnuException extends RuntimeException {

    public ExerciceInconnuException(Long exerciceId) {
        super("L'exercice " + exerciceId + " n'existe pas.");
    }
}
