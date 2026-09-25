package com.kfokam48.kfokam48.session;

public class SessionClotureeException extends RuntimeException {

    public SessionClotureeException() {
        super("La session est clôturée : il n'est plus possible de marquer sa présence.");
    }

    public SessionClotureeException(String message) {
        super(message);
    }
}
