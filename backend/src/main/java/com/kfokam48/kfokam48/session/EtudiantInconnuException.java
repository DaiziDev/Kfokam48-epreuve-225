package com.kfokam48.kfokam48.session;

public class EtudiantInconnuException extends RuntimeException {

    private final Long etudiantId;

    public EtudiantInconnuException(Long etudiantId) {
        super("L'étudiant " + etudiantId + " n'existe pas.");
        this.etudiantId = etudiantId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }
}
