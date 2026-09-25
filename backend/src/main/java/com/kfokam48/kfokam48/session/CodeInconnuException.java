package com.kfokam48.kfokam48.session;

public class CodeInconnuException extends RuntimeException {

    public CodeInconnuException(String code) {
        super("Le code " + code + " ne correspond à aucune session ouverte.");
    }
}
