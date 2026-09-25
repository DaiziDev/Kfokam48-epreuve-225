package com.kfokam48.kfokam48.exercice;

public class AucunRelecteurDisponibleException extends RuntimeException {

    public AucunRelecteurDisponibleException() {
        super("Aucun autre étudiant n'est présent à cette session pour relire l'exercice : réessayez dès qu'un autre présent est enregistré.");
    }
}
