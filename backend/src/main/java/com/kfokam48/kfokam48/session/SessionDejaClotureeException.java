package com.kfokam48.kfokam48.session;

public class SessionDejaClotureeException extends RuntimeException {

    public SessionDejaClotureeException() {
        super("La session est déjà clôturée.");
    }
}
