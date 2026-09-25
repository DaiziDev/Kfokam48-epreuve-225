package com.kfokam48.kfokam48.relecture;

public class RelectureDejaRendueException extends RuntimeException {

    public RelectureDejaRendueException() {
        super("Cette relecture a déjà été rendue : la note est définitive.");
    }
}
