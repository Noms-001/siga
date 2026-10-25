package mg.bank.backend.service.indicateur;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.model.notification.Notification;
import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.repository.notification.NotificationRepository;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;
import mg.bank.backend.service.common.PriorityService;


@Service
@RequiredArgsConstructor
@Slf4j
public class IndicateurRetardNotificationService {

    /** Resource utilisee pour distinguer les notifications de retard. */
    private static final String RESOURCE = "INDICATEUR_RETARD";

    /** Delai anti-doublon (en jours). */
    private static final int DELAI_ANTI_DOUBLON_JOURS = 7;

    private final PriorityService priorityService;

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SuiviIndicateurService suiviIndicateurService;

    @Transactional
    public void notifierIndicateursEnRetard() {
        List<IndicateurEnRetardDTO> enRetard = suiviIndicateurService
                .listerEnRetard(false, true);

        if (enRetard.isEmpty()) {
            log.info("[indicateurs-retard] Aucun indicateur en retard.");
            return;
        }

        Priorite priorite = priorityService.getForNotification();
        if (priorite == null) {
            log.warn("[indicateurs-retard] Aucune priorité active : notifications non envoyées.");
            return;
        }

        int creees = 0;
        for (IndicateurEnRetardDTO dto : enRetard) {
            LocalDateTime depuis = LocalDateTime.now().minusDays(DELAI_ANTI_DOUBLON_JOURS);
            long dejaNotifie = notificationRepository.compterRecentes(
                    dto.getIdIndicateur(), RESOURCE, depuis);
            if (dejaNotifie > 0) {
                continue;
            }

            List<Integer> destinataires = trouverDestinataires(dto.getIdIndicateur());
            if (destinataires.isEmpty()) {
                log.debug("[indicateurs-retard] Indicateur {} sans destinataire.", dto.getCode());
                continue;
            }

            String titre = "Indicateur en retard de saisie";
            String message = construireMessage(dto);

            for (Integer idDestinataire : destinataires) {
                notificationRepository.save(Notification.builder()
                        .titre(titre)
                        .message(message)
                        .dateCreation(LocalDateTime.now())
                        .priorite(priorite)
                        .utilisateur(utilisateurRepository.findById(idDestinataire)
                                .orElseThrow())
                        .idReference(dto.getIdIndicateur())
                        .resource(RESOURCE)
                        .build());
                creees++;
            }
        }

        log.info("[indicateurs-retard] {} notification(s) créée(s) sur {} indicateur(s) en retard.",
                creees, enRetard.size());
    }

    private List<Integer> trouverDestinataires(Integer idIndicateur) {
        return notificationRepository.findDestinatairesIndicateur(idIndicateur);
    }

    private String construireMessage(IndicateurEnRetardDTO dto) {
        if ("JAMAIS_SAISI".equals(dto.getStatut())) {
            return "L'indicateur « " + dto.getCode() + " - " + dto.getDesignation()
                    + " » n'a encore fait l'objet d'aucune saisie. "
                    + "Cadence attendue : " + dto.getFrequenceVerification() + ".";
        }
        return "L'indicateur « " + dto.getCode() + " - " + dto.getDesignation()
                + " » n'a pas été saisi depuis le "
                + formatDate(dto.getDernierePeriodeFin()) + " (cadence : "
                + dto.getFrequenceVerification() + "). "
                + "Échéance dépassée de " + dto.getJoursDeRetard() + " jour(s).";
    }

    private String formatDate(LocalDateTime d) {
        if (d == null) return "—";
        return d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}