package mg.bank.backend.repository.utilisateur;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.utilisateur.PostePermission;
import mg.bank.backend.model.utilisateur.PostePermissionId;

public interface PostePermissionRepository extends JpaRepository<PostePermission, PostePermissionId> {

    /**
     * Postes associés à une permission, avec leur portée.
     *
     * JOIN FETCH sur poste : sans lui, chaque pp.getPoste().getLibelle()
     * déclencherait une requête supplémentaire (N+1). Utilisé par le détail
     * d'une permission.
     */
    @Query("""
            select pp
            from PostePermission pp
            join fetch pp.poste
            where pp.permission.idPermission = :idPermission
            order by pp.poste.libelle
            """)
    List<PostePermission> findByPermissionIdWithPoste(@Param("idPermission") Integer idPermission);

    @Query("""
            select pp
            from PostePermission pp
            join fetch pp.permission
            where pp.poste.idPoste = :idPoste
            order by pp.permission.ressource, pp.permission.action, pp.permission.designation
            """)
    List<PostePermission> findByPosteIdWithPermission(@Param("idPoste") Integer idPoste);


    default Optional<PostePermission> findAssociation(Integer idPoste, Integer idPermission) {
        return findById(new PostePermissionId(idPoste, idPermission));
    }

    @Query("""
            select case when count(pp) > 0 then true else false end
            from PostePermission pp
            where pp.poste.idPoste = :idPoste
              and lower(pp.permission.ressource) = lower(:resource)
              and lower(pp.permission.action) = lower(:action)
              and pp.permission.actif = true
            """)
    boolean existsActivePermission(
            @Param("idPoste") Integer idPoste,
            @Param("resource") String resource,
            @Param("action") String action);

    @Query("""
            select lower(pp.permission.ressource) || ':' || lower(pp.permission.action)
            from PostePermission pp
            where pp.poste.idPoste = :idPoste
              and pp.permission.actif = true
            order by lower(pp.permission.ressource), lower(pp.permission.action)
            """)
    List<String> findActivePermissionKeys(@Param("idPoste") Integer idPoste);
}