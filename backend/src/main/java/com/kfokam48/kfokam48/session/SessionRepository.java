package com.kfokam48.kfokam48.session;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    /** Utilisé par le retry de génération de code en cas de collision d'unicité. */
    boolean existsByCode(String code);

    /** EF2/EF3 : retrouver la session visée par un code de présence. */
    java.util.Optional<SessionEntity> findByCode(String code);

    /**
     * #22 : verrou d'écriture sur la session le temps d'un pointage. Deux pointages
     * simultanés du même étudiant sont sérialisés : le second voit la présence du
     * premier et reçoit 409 DEJA_PRESENT au lieu d'une violation d'unicité (500).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.code = :code")
    java.util.Optional<SessionEntity> verrouillerParCode(@Param("code") String code);

    /** Frontend : sessions d'une promotion, la plus récente d'abord. */
    java.util.List<SessionEntity> findByPromotionIdOrderByOuvertureAtDescIdDesc(Long promotionId);

    /**
     * EF14/RG14 : clôture en une requête toutes les sessions encore ouvertes dont
     * l'expiration est antérieure ou égale à la limite. Contexte vidé après coup
     * pour qu'aucune entité déjà chargée ne garde l'ancien statut.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SessionEntity s set s.statut = :cloturee "
            + "where s.statut = :ouverte and s.expirationAt <= :limite")
    int cloturerExpireesAvant(@Param("limite") Instant limite,
            @Param("ouverte") SessionStatut ouverte, @Param("cloturee") SessionStatut cloturee);
}
