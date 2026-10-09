package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.bank.backend.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    @Query("select new mg.bank.backend.dto.NotificationDTO(n.idNotification, n.titre, n.message, n.dateCreation, n.dateLecture, case when n.resource = 'ACTIVITE' then n.idReference else null end, case when n.resource = 'ACTIVITE' then a.designation else null end, n.priorite.code, n.priorite.libelle) from Notification n left join Activite a on n.resource = 'ACTIVITE' and a.idActivite = n.idReference where n.utilisateur.idUtilisateur = :id order by case when n.dateLecture is null then 0 else 1 end asc, case n.priorite.code when 'CRITIQUE' then 4 when 'HAUTE' then 3 when 'NORMALE' then 2 when 'FAIBLE' then 1 else 0 end desc, n.dateCreation desc, n.idNotification desc")
    List<mg.bank.backend.dto.NotificationDTO> lister(@Param("id") Integer idUtilisateur);

    @Query(value = "select new mg.bank.backend.dto.NotificationDTO(n.idNotification, n.titre, n.message, n.dateCreation, n.dateLecture, case when n.resource = 'ACTIVITE' then n.idReference else null end, case when n.resource = 'ACTIVITE' then a.designation else null end, n.priorite.code, n.priorite.libelle) from Notification n left join Activite a on n.resource = 'ACTIVITE' and a.idActivite = n.idReference where n.utilisateur.idUtilisateur = :id and (:search is null or lower(n.titre) like concat('%', lower(:search), '%') or lower(n.message) like concat('%', lower(:search), '%') or lower(a.designation) like concat('%', lower(:search), '%')) and (:lecture = 'TOUTES' or (:lecture = 'NON_LUES' and n.dateLecture is null) or (:lecture = 'LUES' and n.dateLecture is not null)) and (:priorite is null or n.priorite.code = :priorite) order by case when n.dateLecture is null then 0 else 1 end asc, case n.priorite.code when 'CRITIQUE' then 4 when 'HAUTE' then 3 when 'NORMALE' then 2 when 'FAIBLE' then 1 else 0 end desc, n.dateCreation desc, n.idNotification desc",
            countQuery = "select count(n) from Notification n left join Activite a on n.resource = 'ACTIVITE' and a.idActivite = n.idReference where n.utilisateur.idUtilisateur = :id and (:search is null or lower(n.titre) like concat('%', lower(:search), '%') or lower(n.message) like concat('%', lower(:search), '%') or lower(a.designation) like concat('%', lower(:search), '%')) and (:lecture = 'TOUTES' or (:lecture = 'NON_LUES' and n.dateLecture is null) or (:lecture = 'LUES' and n.dateLecture is not null)) and (:priorite is null or n.priorite.code = :priorite)")
    Page<mg.bank.backend.dto.NotificationDTO> listerPage(
            @Param("id") Integer idUtilisateur,
            @Param("search") String search,
            @Param("lecture") String lecture,
            @Param("priorite") String priorite,
            Pageable pageable);

    long countByUtilisateur_IdUtilisateurAndDateLectureIsNull(Integer idUtilisateur);

    @Modifying
    @Query("update Notification n set n.dateLecture = CURRENT_TIMESTAMP where n.utilisateur.idUtilisateur = :id and n.dateLecture is null")
    int marquerToutesLues(@Param("id") Integer idUtilisateur);
    Optional<Notification> findByIdNotificationAndUtilisateur_IdUtilisateur(Integer id, Integer idUtilisateur);
}
