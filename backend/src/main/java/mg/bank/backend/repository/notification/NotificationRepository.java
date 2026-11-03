package mg.bank.backend.repository.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.dto.notification.NotificationDTO;
import mg.bank.backend.model.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
  @Query("select new mg.bank.backend.dto.notification.NotificationDTO(n.idNotification, n.titre, n.message, n.dateCreation, n.dateLecture, case when n.resource = 'ACTIVITE' then n.idReference else null end, case when n.resource = 'ACTIVITE' then a.designation else null end, n.priorite.code, n.priorite.libelle) from Notification n left join Activite a on n.resource = 'ACTIVITE' and a.idActivite = n.idReference where n.utilisateur.idUtilisateur = :id order by case when n.dateLecture is null then 0 else 1 end asc, case n.priorite.code when 'CRITIQUE' then 4 when 'HAUTE' then 3 when 'NORMALE' then 2 when 'FAIBLE' then 1 else 0 end desc, n.dateCreation desc, n.idNotification desc")
  List<mg.bank.backend.dto.notification.NotificationDTO> lister(@Param("id") Integer idUtilisateur);

  @Query(value = """
    select new mg.bank.backend.dto.notification.NotificationDTO(
        n.idNotification, n.titre, n.message, n.dateCreation, n.dateLecture,
        case when n.resource = 'ACTIVITE' then n.idReference else null end,
        case when n.resource = 'ACTIVITE' then a.designation else null end,
        n.priorite.code, n.priorite.libelle)
    from Notification n
    left join Activite a
        on n.resource = 'ACTIVITE' and a.idActivite = n.idReference
    where n.utilisateur.idUtilisateur = :id
      and (
        :search is null
        or lower(n.titre)      like concat('%', lower(cast(:search as string)), '%')
        or lower(n.message)    like concat('%', lower(cast(:search as string)), '%')
        or lower(a.designation) like concat('%', lower(cast(:search as string)), '%')
      )
      and (
        :lecture = 'TOUTES'
        or (:lecture = 'NON_LUES' and n.dateLecture is null)
        or (:lecture = 'LUES'     and n.dateLecture is not null)
      )
      and (
        :filtrerPriorites = false
        or n.priorite.code in :priorites
      )
    order by
        case when n.dateLecture is null then 0 else 1 end asc,
        case n.priorite.code
            when 'CRITIQUE' then 4
            when 'HAUTE'    then 3
            when 'NORMALE'  then 2
            when 'FAIBLE'   then 1
            else 0
        end desc,
        n.dateCreation desc,
        n.idNotification desc
    """, countQuery = """
    select count(n)
    from Notification n
    left join Activite a
        on n.resource = 'ACTIVITE' and a.idActivite = n.idReference
    where n.utilisateur.idUtilisateur = :id
      and (
        :search is null
        or lower(n.titre)      like concat('%', lower(cast(:search as string)), '%')
        or lower(n.message)    like concat('%', lower(cast(:search as string)), '%')
        or lower(a.designation) like concat('%', lower(cast(:search as string)), '%')
      )
      and (
        :lecture = 'TOUTES'
        or (:lecture = 'NON_LUES' and n.dateLecture is null)
        or (:lecture = 'LUES'     and n.dateLecture is not null)
      )
      and (
        :filtrerPriorites = false
        or n.priorite.code in :priorites
      )
    """)
Page<NotificationDTO> listerPage(
        @Param("id") Integer idUtilisateur,
        @Param("search") String search,
        @Param("lecture") String lecture,
        @Param("filtrerPriorites") boolean filtrerPriorites,
        @Param("priorites") List<String> priorites,
        Pageable pageable);

  long countByUtilisateur_IdUtilisateurAndDateLectureIsNull(Integer idUtilisateur);

  @Modifying
  @Query("update Notification n set n.dateLecture = CURRENT_TIMESTAMP where n.utilisateur.idUtilisateur = :id and n.dateLecture is null")
  int marquerToutesLues(@Param("id") Integer idUtilisateur);

  Optional<Notification> findByIdNotificationAndUtilisateur_IdUtilisateur(Integer id, Integer idUtilisateur);

  @Query("""
      SELECT COUNT(n) FROM Notification n
      WHERE n.idReference = :idReference
        AND n.resource = :resource
        AND n.dateCreation > :depuis
      """)
  long compterRecentes(
      @Param("idReference") Integer idReference,
      @Param("resource") String resource,
      @Param("depuis") LocalDateTime depuis);

  /**
   * Utilisateurs destinataires d'une notification sur un indicateur :
   * RESPONSABLE et SUPPLEANT actifs des activites liees a cet indicateur.
   * Distinct pour ne pas envoyer plusieurs fois au meme utilisateur.
   */
  @Query(value = """
      SELECT DISTINCT aa.id_utilisateur
      FROM activite_indicateur ai
      JOIN affectation_activite aa ON aa.id_activite = ai.id_activite
      JOIN role r ON r.id_role = aa.id_role
      WHERE ai.id_indicateur = :idIndicateur
        AND aa.date_desaffectation IS NULL
        AND r.code IN ('RESPONSABLE', 'SUPPLEANT')
      """, nativeQuery = true)
  List<Integer> findDestinatairesIndicateur(@Param("idIndicateur") Integer idIndicateur);

  /**
   * Notifications urgentes en attente d'envoi email.
   *
   * <p>
   * Trois conditions cumulatives : priorité dans la liste configurée,
   * pas encore envoyée, moins de 3 tentatives. Le JOIN FETCH évite le
   * N+1 : chaque notification est traitée avec son utilisateur et sa
   * priorité chargés en une seule requête.
   * </p>
   */
  @Query("""
      SELECT n FROM Notification n
      JOIN FETCH n.utilisateur u
      JOIN FETCH n.priorite p
      WHERE p.code IN :priorites
        AND n.dateEnvoiEmail IS NULL
        AND n.nombreTentativesEmail < :maxTentatives
        AND u.email IS NOT NULL
        AND u.email <> ''
      ORDER BY n.dateCreation ASC
      """)
  List<Notification> findUrgentesNonEnvoyees(
      @Param("priorites") List<String> priorites,
      @Param("maxTentatives") int maxTentatives);

  /**
   * Marque une notification comme envoyée.
   * Utilisée après succès SMTP.
   */
  @Modifying
  @Query("""
      UPDATE Notification n
         SET n.dateEnvoiEmail = :date,
             n.nombreTentativesEmail = n.nombreTentativesEmail + 1
       WHERE n.idNotification = :id
      """)
  void marquerEnvoyee(@Param("id") Integer id, @Param("date") LocalDateTime date);

  /**
   * Incrémente le compteur après échec.
   * L'update atomique évite d'écraser des modifications concurrentes.
   */
  @Modifying
  @Query("""
      UPDATE Notification n
         SET n.nombreTentativesEmail = n.nombreTentativesEmail + 1
       WHERE n.idNotification = :id
      """)
  void incrementerTentatives(@Param("id") Integer id);
}
