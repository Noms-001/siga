package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.bank.backend.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    @Query("select new mg.bank.backend.dto.NotificationDTO(n.idNotification, n.titre, n.message, n.dateCreation, n.dateLecture, case when n.resource = 'ACTIVITE' then n.idReference else null end, case when n.resource = 'ACTIVITE' then a.designation else null end) from Notification n left join Activite a on n.resource = 'ACTIVITE' and a.idActivite = n.idReference where n.utilisateur.idUtilisateur = :id order by n.dateCreation desc")
    List<mg.bank.backend.dto.NotificationDTO> lister(@Param("id") Integer idUtilisateur);

    long countByUtilisateur_IdUtilisateurAndDateLectureIsNull(Integer idUtilisateur);
    Optional<Notification> findByIdNotificationAndUtilisateur_IdUtilisateur(Integer id, Integer idUtilisateur);
}
