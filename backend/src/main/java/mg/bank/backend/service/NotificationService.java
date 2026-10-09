package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.NotificationDTO;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.model.Notification;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.NotificationRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.UtilisateurRepository;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PrioriteRepository prioriteRepository;

    @Transactional
    public void notifierEtape(Activite activite, EtapeValidation etape, String titre, String message) {
        if (etape == null) return;
        List<Utilisateur> destinataires = utilisateurRepository.findDestinatairesEtape(
                etape.getIdEtapeValidation(), activite.getService().getIdService(), activite.getService().getDepartement().getIdDepartement());
        destinataires.forEach(u -> creer(activite, u, titre, message));
    }

    @Transactional
    public void notifierUtilisateur(Activite activite, Utilisateur utilisateur, String titre, String message) {
        if (utilisateur != null) creer(activite, utilisateur, titre, message);
    }

    private void creer(Activite activite, Utilisateur utilisateur, String titre, String message) {
        notificationRepository.save(Notification.builder()
                .titre(titre).message(message).dateCreation(LocalDateTime.now())
                .priorite(activite.getPriorite()).utilisateur(utilisateur)
                .idReference(activite.getIdActivite()).resource("ACTIVITE").build());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> lister(Integer idUtilisateur) { return notificationRepository.lister(idUtilisateur); }
    @Transactional(readOnly = true)
    public Page<NotificationDTO> listerPage(
            Integer idUtilisateur, String search, String lecture, String priorite, Pageable pageable) {
        return notificationRepository.listerPage(idUtilisateur, search, lecture, priorite, pageable);
    }
    @Transactional(readOnly = true)
    public long compterNonLues(Integer idUtilisateur) { return notificationRepository.countByUtilisateur_IdUtilisateurAndDateLectureIsNull(idUtilisateur); }
    @Transactional
    public boolean marquerLue(Integer idUtilisateur, Integer idNotification) {
        return notificationRepository.findByIdNotificationAndUtilisateur_IdUtilisateur(idNotification, idUtilisateur)
                .map(n -> { if (n.getDateLecture() == null) n.setDateLecture(LocalDateTime.now()); return true; })
                .orElse(false);
    }
    @Transactional
    public int marquerToutesLues(Integer idUtilisateur) { return notificationRepository.marquerToutesLues(idUtilisateur); }
}
