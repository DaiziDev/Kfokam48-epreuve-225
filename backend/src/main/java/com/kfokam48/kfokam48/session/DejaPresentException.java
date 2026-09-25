package com.kfokam48.kfokam48.session;

public class DejaPresentException extends RuntimeException {

    public DejaPresentException() {
        super("Cet étudiant est déjà marqué présent à cette session.");
    }
}
