package mg.bank.backend.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.bank.backend.service.notification.NotificationEmailDispatchService;

/**
 * Planification de l'envoi email des notifications urgentes.
 *
 * <p>
 * Désactivable via {@code app.notification.email-dispatch-enabled=false}
 * (utile en tests ou pour couper le service en production sans redéployer).
 * </p>
 *
 * <p>
 * Démarrage différé de 60s : le backend a le temps de finir son
 * initialisation (JPA, sécurité, mail sender) avant le premier passage.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.email-dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEmailDispatchScheduler {

    private final NotificationEmailDispatchService dispatchService;

    @Scheduled(initialDelayString = "${app.notification.email-dispatch-initial-delay-ms:60000}", fixedDelayString = "${app.notification.email-dispatch-interval-ms:300000}")
    public void executer() {
        try {
            dispatchService.dispatcherUrgentes();
        } catch (Exception e) {
            log.error("Erreur scheduler email notifications", e);
        }
    }
}