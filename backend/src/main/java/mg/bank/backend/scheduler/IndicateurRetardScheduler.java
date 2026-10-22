package mg.bank.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.bank.backend.service.IndicateurRetardNotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndicateurRetardScheduler {

    private final IndicateurRetardNotificationService notificationService;

    /**
     * Tous les jours a 8h00 (heure serveur).
     *
     * Le cron "0 0 8 * * *" = seconde 0, minute 0, heure 8, tous les
     * jours, tous les mois, tous les jours de la semaine.
     *
     * Le scheduler n'est PAS transactionnel lui-meme : la methode appel
     * notifierIndicateursEnRetard() l'est (@Transactional). Si elle
     * echoue, tout est rollback et une ligne d'erreur est loggee au
     * prochain tick.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void executerNotifierRetards() {
        log.info("[indicateurs-retard] Démarrage du job quotidien.");
        try {
            notificationService.notifierIndicateursEnRetard();
        } catch (Exception e) {
            log.error("[indicateurs-retard] Echec du job : {}", e.getMessage(), e);
        }
    }
}