package br.com.devready.worker.session;

import br.com.devready.application.gamification.usecase.AwardAchievementsUseCase;
import br.com.devready.application.session.dto.ExpiryRound;
import br.com.devready.application.session.usecase.ExpireSessionsUseCase;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionExpiryScheduler.class);

    private final ExpireSessionsUseCase expireSessions;
    private final AwardAchievementsUseCase awardAchievements;

    public SessionExpiryScheduler(
            ExpireSessionsUseCase expireSessions, AwardAchievementsUseCase awardAchievements) {
        this.expireSessions = expireSessions;
        this.awardAchievements = awardAchievements;
    }

    @Scheduled(
            fixedDelayString = "${devready.sessions.expiry-delay-ms:60000}",
            initialDelayString = "${devready.sessions.expiry-delay-ms:60000}")
    public void expire() {
        ExpiryRound round = expireSessions.execute();
        if (round.expired() == 0 && round.closed().isEmpty()) {
            log.debug("Nenhuma sessão venceu nem foi fechada nesta rodada");
            return;
        }
        log.info(
                "Sessões vencidas nesta rodada: {}; fechadas: {}",
                round.expired(),
                round.closed().size());
        for (Session session : round.closed()) {
            log.info("Sessão {} fechada como {}", session.id(), session.status());
            if (session.status() == SessionStatus.COMPLETED) {
                try {
                    awardAchievements.execute(session.userId(), session.id());
                } catch (RuntimeException e) {
                    log.warn("Sessão {} corrigida, mas as conquistas falharam", session.id(), e);
                }
            }
        }
    }
}
