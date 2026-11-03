package mg.bank.backend.service.notification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.bank.backend.model.notification.Notification;
import mg.bank.backend.model.parametre.Parametre;
import mg.bank.backend.repository.notification.NotificationRepository;
import mg.bank.backend.repository.parametre.ParametreRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailDispatchService {

    private static final String CODE_PRIORITES = "NOTIFICATION_EMAIL_PRIORITES";
    private static final String DEFAULT_PRIORITES = "CRITIQUE,HAUTE";
    private static final int MAX_TENTATIVES = 3;

    private final NotificationRepository notificationRepository;
    private final ParametreRepository parametreRepository;
    private final EmailService emailService;

    /**
     * TransactionTemplate : permet de créer une transaction COURTE juste
     * autour de chaque UPDATE. L'appel SMTP reste HORS transaction.
     *
     * Pourquoi pas @Transactional sur dispatcherUrgentes() ?
     * Un appel SMTP peut prendre 1 à 30 secondes (timeout). Si la transaction
     * englobe tout le lot, une connexion PostgreSQL reste immobilisée pendant
     * la durée totale du lot (8 × ~2s = 16s pour 8 emails). TransactionTemplate
     * évite ce verrouillage inutile.
     */
    private final TransactionTemplate transactionTemplate;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ------------------------------------------------------------------
    // Point d'entrée
    // ------------------------------------------------------------------

    public int dispatcherUrgentes() {

        List<String> priorites = lirePrioritesUrgentes();

        List<Notification> aEnvoyer;
        try {
            aEnvoyer = notificationRepository.findUrgentesNonEnvoyees(
                    priorites, MAX_TENTATIVES);
        } catch (Exception e) {
            log.error("Chargement des notifications urgentes impossible", e);
            return 0;
        }

        if (aEnvoyer.isEmpty()) {
            return 0;
        }

        String lien = frontendUrl + "/notifications/urgentes";
        int envoyes = 0;

        for (Notification n : aEnvoyer) {
            if (envoyerUne(n, lien))
                envoyes++;
        }

        if (envoyes > 0) {
            log.info("Emails notifications urgentes : {} envoyé(s) / {}",
                    envoyes, aEnvoyer.size());
        }
        return envoyes;
    }

    // ------------------------------------------------------------------
    // Envoi individuel
    // ------------------------------------------------------------------

    private boolean envoyerUne(Notification n, String lien) {
        Integer id = n.getIdNotification();
        int tentativesAvant = n.getNombreTentativesEmail() == null
                ? 0
                : n.getNombreTentativesEmail();

        try {
            // 1. Envoi SMTP — HORS transaction
            emailService.sendNotificationUrgente(
                    n.getUtilisateur(),
                    n.getTitre(),
                    n.getMessage(),
                    lien);

            // 2. Marquer envoyée — transaction courte
            transactionTemplate
                    .executeWithoutResult(status -> notificationRepository.marquerEnvoyee(id, LocalDateTime.now()));

            if (tentativesAvant > 0) {
                log.info("Notification {} envoyée après {} échec(s)",
                        id, tentativesAvant);
            }
            return true;

        } catch (Exception e) {
            int tentativesApres = tentativesAvant + 1;
            final boolean abandon = tentativesApres >= MAX_TENTATIVES;

            // Incrément du compteur — transaction courte
            transactionTemplate.executeWithoutResult(status -> {
                notificationRepository.incrementerTentatives(id);
                if (abandon) {
                    notificationRepository.marquerEnvoyee(id, LocalDateTime.now());
                }
            });

            if (abandon) {
                log.error("Notification {} abandonnée après {} tentatives : {}",
                        id, tentativesApres, e.getMessage());
            } else {
                log.warn("Échec envoi notification {} (tentative {}/{}) : {}",
                        id, tentativesApres, MAX_TENTATIVES, e.getMessage());
            }
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Paramètres
    // ------------------------------------------------------------------

    private List<String> lirePrioritesUrgentes() {
        return parametreRepository.findByCodeAndActifTrue(CODE_PRIORITES)
                .map(Parametre::getValeur)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> Arrays.stream(v.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList())
                .filter(l -> !l.isEmpty())
                .orElseGet(() -> List.of(DEFAULT_PRIORITES.split(",")));
    }
}