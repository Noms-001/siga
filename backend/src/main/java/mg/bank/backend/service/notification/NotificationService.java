package mg.bank.backend.service.notification;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.common.UtilisateurResumeDTO;
import mg.bank.backend.dto.notification.NotificationDTO;
import mg.bank.backend.dto.notification.NotificationDetailDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.notification.Notification;
import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.model.validation.EtapeValidation;
import mg.bank.backend.repository.notification.NotificationDetailRepository;
import mg.bank.backend.repository.notification.NotificationRepository;
import mg.bank.backend.repository.referentiel.PrioriteRepository;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;
import mg.bank.backend.service.common.PorteeService;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationDetailRepository notificationDetailRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PrioriteRepository prioriteRepository;
    private final PorteeService porteeService;

    @Transactional
    public void notifierEtape(Activite activite, EtapeValidation etape, String titre, String message) {
        if (etape == null)
            return;
        List<Utilisateur> destinataires = utilisateurRepository.findDestinatairesEtape(
                etape.getIdEtapeValidation(), activite.getService().getIdService(),
                activite.getService().getDepartement().getIdDepartement());
        destinataires.forEach(u -> creer(activite, u, titre, message));
    }

    @Transactional
    public void notifierUtilisateur(Activite activite, Utilisateur utilisateur, String titre, String message) {
        if (utilisateur != null)
            creer(activite, utilisateur, titre, message);
    }

    private void creer(Activite activite, Utilisateur utilisateur, String titre, String message) {
        Priorite priorite = activite.getPriorite();
        if (priorite == null) {
            priorite = prioriteRepository.findByCodeIgnoreCase("NORMALE").orElse(null);
        }
        notificationRepository.save(Notification.builder()
                .titre(titre).message(message).dateCreation(LocalDateTime.now())
                .priorite(priorite).utilisateur(utilisateur)
                .idReference(activite.getIdActivite()).resource("ACTIVITE").build());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> lister(Integer idUtilisateur) {
        return notificationRepository.lister(idUtilisateur);
    }

    private static final List<String> SENTINEL_AUCUN_FILTRE = List.of("__NONE__");

    @Transactional(readOnly = true)
    public Page<NotificationDTO> listerPage(
            Integer idUtilisateur,
            String search,
            String lecture,
            List<String> priorites,
            Pageable pageable) {

        boolean filtrer = priorites != null && !priorites.isEmpty();

        // IN () et IN (null) cassent en JPQL. On garantit une liste non vide.
        List<String> safe = filtrer ? priorites : SENTINEL_AUCUN_FILTRE;

        return notificationRepository.listerPage(
                idUtilisateur, search, lecture, filtrer, safe, pageable);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Integer idUtilisateur) {
        return notificationRepository.countByUtilisateur_IdUtilisateurAndDateLectureIsNull(idUtilisateur);
    }

    @Transactional
    public boolean marquerLue(Integer idUtilisateur, Integer idNotification) {
        return notificationRepository.findByIdNotificationAndUtilisateur_IdUtilisateur(idNotification, idUtilisateur)
                .map(n -> {
                    if (n.getDateLecture() == null)
                        n.setDateLecture(LocalDateTime.now());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int marquerToutesLues(Integer idUtilisateur) {
        return notificationRepository.marquerToutesLues(idUtilisateur);
    }

    @Transactional(readOnly = true)
    public NotificationDetailDTO detail(Integer idNotification, Integer idUtilisateur) {

        Notification n = notificationRepository
                .findByIdNotificationAndUtilisateur_IdUtilisateur(idNotification, idUtilisateur)
                .orElseThrow(() -> new ApiException(
                        "Notification introuvable", HttpStatus.NOT_FOUND));

        NotificationDetailDTO.NotificationDetailDTOBuilder b = NotificationDetailDTO.builder()
                .id(n.getIdNotification())
                .titre(n.getTitre())
                .message(n.getMessage())
                .dateCreation(n.getDateCreation())
                .dateLecture(n.getDateLecture())
                .priorite(n.getPriorite() == null ? null
                        : ReferenceDTO.builder()
                                .id(n.getPriorite().getIdPriorite())
                                .code(n.getPriorite().getCode())
                                .libelle(n.getPriorite().getLibelle())
                                .build())
                .utilisateur(n.getUtilisateur() == null ? null
                        : UtilisateurResumeDTO.videSi(
                                n.getUtilisateur().getIdUtilisateur(),
                                n.getUtilisateur().getNom(),
                                n.getUtilisateur().getPrenom()))
                .resource(n.getResource())
                .idReference(n.getIdReference())
                .contexte(Map.of());

        if (n.getResource() == null || n.getIdReference() == null) {
            return b.build();
        }

        switch (n.getResource()) {
            case "ACTIVITE" -> enrichirActivite(b, n.getIdReference());
            case "SOUS_ACTIVITE" -> enrichirSousActivite(b, n.getIdReference());
            case "PLAN_ACTION" -> enrichirPlanAction(b, n.getIdReference());
            case "INDICATEUR" -> enrichirIndicateur(b, n.getIdReference());
            // autre cas : entité non gérée, DTO reste minimal
        }

        return b.build();
    }

    private void enrichirActivite(NotificationDetailDTO.NotificationDetailDTOBuilder b, Integer id) {
        if (!porteeService.activiteAccessible(id)) {
            b.entiteDisponible(false);
            return;
        }
        notificationDetailRepository.findActiviteContexte(id).ifPresentOrElse(
                row -> {
                    Map<String, String> ctx = new LinkedHashMap<>();
                    if (row.getStatutLibelle() != null)
                        ctx.put("Statut", row.getStatutLibelle());
                    if (row.getAvancement() != null)
                        ctx.put("Avancement", formatPct(row.getAvancement()));
                    if (row.getDateFinPrevue() != null)
                        ctx.put("Échéance prévue", formatDate(row.getDateFinPrevue()));

                    b.entiteDisponible(true)
                            .libelleReference(row.getCode() + " — " + row.getDesignation())
                            .urlFront("/activites/" + id)
                            .contexte(ctx);
                },
                () -> b.entiteDisponible(false));
    }

    private void enrichirSousActivite(NotificationDetailDTO.NotificationDetailDTOBuilder b, Integer id) {
        notificationDetailRepository.findSousActiviteContexte(id).ifPresentOrElse(
                row -> {
                    if (!porteeService.activiteAccessible(row.getIdActivite())) {
                        b.entiteDisponible(false);
                        return;
                    }
                    Map<String, String> ctx = new LinkedHashMap<>();
                    if (row.getStatutLibelle() != null)
                        ctx.put("Statut", row.getStatutLibelle());
                    if (row.getAvancement() != null)
                        ctx.put("Avancement", formatPct(row.getAvancement()));
                    if (row.getDateFinPrevue() != null)
                        ctx.put("Échéance prévue", formatDate(row.getDateFinPrevue()));
                    String resp = formatNom(row.getResponsablePrenom(), row.getResponsableNom());
                    if (resp != null)
                        ctx.put("Responsable", resp);

                    b.entiteDisponible(true)
                            .libelleReference(row.getCode() + " — " + row.getDesignation())
                            .urlFront("/activites/" + row.getIdActivite() + "/sous-activites/" + id)
                            .contexte(ctx);
                },
                () -> b.entiteDisponible(false));
    }

    private void enrichirPlanAction(NotificationDetailDTO.NotificationDetailDTOBuilder b, Integer id) {
        if (!porteeService.planActionAccessible(id)) {
            b.entiteDisponible(false);
            return;
        }
        notificationDetailRepository.findPlanActionContexte(id).ifPresentOrElse(
                row -> {
                    Map<String, String> ctx = new LinkedHashMap<>();
                    if (row.getStatutLibelle() != null)
                        ctx.put("Statut", row.getStatutLibelle());
                    if (row.getAvancement() != null)
                        ctx.put("Avancement", formatPct(row.getAvancement()));
                    if (row.getDateFinPrevue() != null)
                        ctx.put("Échéance prévue", formatDate(row.getDateFinPrevue()));

                    b.entiteDisponible(true)
                            .libelleReference(row.getCode() + " — " + row.getDesignation())
                            .urlFront("/plans-action/" + id)
                            .contexte(ctx);
                },
                () -> b.entiteDisponible(false));
    }

    private void enrichirIndicateur(NotificationDetailDTO.NotificationDetailDTOBuilder b, Integer id) {
        if (!porteeService.indicateurAccessible(id)) {
            b.entiteDisponible(false);
            return;
        }
        notificationDetailRepository.findIndicateurContexte(id).ifPresentOrElse(
                row -> {
                    Map<String, String> ctx = new LinkedHashMap<>();
                    if (row.getValeurCourante() != null)
                        ctx.put("Valeur courante", row.getValeurCourante().toPlainString());
                    if (row.getSeuilMin() != null)
                        ctx.put("Seuil minimum", row.getSeuilMin().toPlainString());
                    if (row.getSeuilMax() != null)
                        ctx.put("Seuil maximum", row.getSeuilMax().toPlainString());
                    if (row.getPeriodeDebut() != null && row.getPeriodeFin() != null) {
                        ctx.put("Période", formatDate(row.getPeriodeDebut().toLocalDate())
                                + " → " + formatDate(row.getPeriodeFin().toLocalDate()));
                    }

                    b.entiteDisponible(true)
                            .libelleReference(row.getCode() + " — " + row.getDesignation())
                            .urlFront("/indicateurs/" + id)
                            .contexte(ctx);
                },
                () -> b.entiteDisponible(false));
    }

    private static String formatPct(Double v) {
        return Math.round(v) + " %";
    }

    private static String formatDate(java.time.LocalDate d) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(d);
    }

    private static String formatNom(String prenom, String nom) {
        if ((prenom == null || prenom.isBlank()) && (nom == null || nom.isBlank()))
            return null;
        return ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
    }
}
