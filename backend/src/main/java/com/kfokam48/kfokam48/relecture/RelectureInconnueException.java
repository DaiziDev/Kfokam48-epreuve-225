package com.kfokam48.kfokam48.relecture;

public class RelectureInconnueException extends RuntimeException {

    public RelectureInconnueException(Long relectureId) {
        super("La relecture " + relectureId + " n'existe pas.");
    }
}
