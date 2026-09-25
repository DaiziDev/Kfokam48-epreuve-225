package com.kfokam48.kfokam48.relecture;

public class NoteInvalideException extends RuntimeException {

    public NoteInvalideException() {
        super("La note doit être un nombre entier compris entre 0 et 20.");
    }
}
