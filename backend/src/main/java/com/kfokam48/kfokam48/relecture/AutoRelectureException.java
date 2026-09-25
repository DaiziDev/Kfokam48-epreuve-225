package com.kfokam48.kfokam48.relecture;

public class AutoRelectureException extends RuntimeException {

    public AutoRelectureException() {
        super("Vous ne pouvez pas relire votre propre exercice.");
    }
}
