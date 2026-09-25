package com.kfokam48.kfokam48.session;

public class CodeExpireException extends RuntimeException {

    public CodeExpireException() {
        super("Le code de présence a expiré.");
    }
}
