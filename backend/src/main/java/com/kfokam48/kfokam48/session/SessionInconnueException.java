package com.kfokam48.kfokam48.session;

public class SessionInconnueException extends RuntimeException {

    private final Long sessionId;

    public SessionInconnueException(Long sessionId) {
        super("La session " + sessionId + " n'existe pas.");
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
