package com.kfokam48.kfokam48.session;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<EtudiantEntity, Long> {

    /** Sélecteur d'identité du frontend : étudiants d'une promotion, triés par nom. */
    java.util.List<EtudiantEntity> findByPromotionIdOrderByNomAscIdAsc(Long promotionId);
}
