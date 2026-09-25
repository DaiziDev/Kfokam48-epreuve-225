package com.kfokam48.kfokam48.session;

public class SessionDejaOuverteException extends RuntimeException {

    public SessionDejaOuverteException() {
        super("La session est déjà ouverte.");
    }
}
