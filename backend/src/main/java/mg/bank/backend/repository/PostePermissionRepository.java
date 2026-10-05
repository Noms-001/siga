package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.PostePermission;
import mg.bank.backend.model.PostePermissionId;

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

    /**
     * Permissions associées à un poste, avec leur portée.
     *
     * JOIN FETCH sur permission : sans lui, la liste des permissions d'un
     * poste déclencherait une requête par ligne affichée.
     */
    @Query("""
            select pp
            from PostePermission pp
            join fetch pp.permission
            where pp.poste.idPoste = :idPoste
            order by pp.permission.ressource, pp.permission.action, pp.permission.designation
            """)
    List<PostePermission> findByPosteIdWithPermission(@Param("idPoste") Integer idPoste);

    /**
     * Récupère l'association depuis la clé composite.
     *
     * Méthode par défaut : la clé composite est un objet, et Spring Data
     * sait naviguer dedans via `findById`. Une méthode dérivée explicite
     * (findByIdPosteAndIdPermission) fonctionnerait aussi mais serait plus
     * verbeuse à lire.
     */
    default Optional<PostePermission> findAssociation(Integer idPoste, Integer idPermission) {
        return findById(new PostePermissionId(idPoste, idPermission));
    }
}