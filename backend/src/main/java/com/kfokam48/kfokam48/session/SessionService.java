package com.kfokam48.kfokam48.session;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier EF1 : ouverture de session avec code de présence éphémère.
 * RG1 : expiration = ouverture + 15 minutes, calculée depuis le Clock injecté.
 */
@Service
public class SessionService {

    /**
     * Alphabet sans ambiguïté visuelle : pas de O/0, I/1, L — le code est tapé
     * à la main sur mobile (ENF1).
     */
    private static final String ALPHABET_CODE = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR_CODE = 6;
    private static final Duration DUREE_CODE = Duration.ofMinutes(15); // RG1
    private static final int MAX_TENTATIVES = 10;

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final Clock horloge;
    private final SecureRandom aleatoire = new SecureRandom();

    public SessionService(SessionRepository sessions, PromotionRepository promotions, Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.horloge = horloge;
    }

    @Transactional
    public SessionCreee ouvrirSession(String titre, Long promotionId) {
        PromotionEntity promotion = promotions.findById(promotionId)
                .orElseThrow(() -> new PromotionInconnueException(promotionId));

        Instant ouvertureAt = horloge.instant();
        Instant expirationAt = ouvertureAt.plus(DUREE_CODE);

        SessionEntity session = new SessionEntity();
        session.setPromotion(promotion);
        session.setTitre(titre);
        session.setOuvertureAt(ouvertureAt);
        session.setExpirationAt(expirationAt);
        session.setStatut(SessionStatut.OUVERTE);
        session.setCode(genererCodeUnique());

        sessions.save(session);

        return new SessionCreee(session.getId(), session.getCode(),
                session.getOuvertureAt(), session.getExpirationAt(), session.getStatut());
    }

    /**
     * Génération avec retry : la contrainte d'unicité en base reste l'arbitre final,
     * existsByCode réduit la probabilité de collision au bruit résiduel.
     */
    private String genererCodeUnique() {
        for (int tentative = 0; tentative < MAX_TENTATIVES; tentative++) {
            String code = genererCode();
            if (!sessions.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Impossible de générer un code de session unique après "
                + MAX_TENTATIVES + " tentatives");
    }

    private String genererCode() {
        StringBuilder sb = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            sb.append(ALPHABET_CODE.charAt(aleatoire.nextInt(ALPHABET_CODE.length())));
        }
        return sb.toString();
    }

    public record SessionCreee(Long id, String code, Instant ouvertureAt,
            Instant expirationAt, SessionStatut statut) {
    }
}
