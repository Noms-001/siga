package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.model.Notification;
import mg.bank.backend.model.Priorite;
import mg.bank.backend.repository.NotificationRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.UtilisateurRepository;

/**
 * Cree les notifications pour les indicateurs dont la cadence de saisie
 * est depassee.
 *
 * <p>Ne pas confondre avec le trigger {@code notify_indicateur_seuil} :
 * ce dernier se declenche a l'INSERTION d'une valeur hors seuil ; ici, on
 * reagit au <b>temps qui passe</b> — ce qu'un trigger SQL ne sait pas
 * faire. Le mecanisme est donc un {@code @Scheduled} quotidien.</p>
 *
 * <p>Destinataires : les RESPONSABLE et SUPPLEANT actifs des activites
 * liees a l'indicateur (meme logique que le trigger de seuil).</p>
 *
 * <p>Anti-doublon : une notification n'est pas renvoyee si une
 * notification de meme type et meme cible a deja ete creee dans les
 * 7 derniers jours. Ce delai evite d'inonder le destinataire tout en
 * rappelant l'echeance chaque semaine.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndicateurRetardNotificationService {

    /** Resource utilisee pour distinguer les notifications de retard. */
    private static final String RESOURCE = "INDICATEUR_RETARD";

    /** Delai anti-doublon (en jours). */
    private static final int DELAI_ANTI_DOUBLON_JOURS = 7;

    private final NotificationRepository notificationRepository;
    private final PrioriteRepository prioriteRepository;
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

        Priorite priorite = trouverPriorite();
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

    private Priorite trouverPriorite() {
        // Convention : la priorité HAUTE si elle existe et est active,
        // sinon la première priorité active par id.
        return prioriteRepository.findByCodeIgnoreCase("HAUTE")
                .filter(Priorite::getActif)
                .orElseGet(() -> prioriteRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActif()))
                        .findFirst()
                        .orElse(null));
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