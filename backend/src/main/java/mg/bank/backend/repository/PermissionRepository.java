package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    @Query("select distinct p.ressource from Permission p order by p.ressource")
    List<String> findDistinctRessources();

    @Query("select distinct p.action from Permission p order by p.action")
    List<String> findDistinctActions();

    @Query("""
            select p from Permission p
            where (:actif is null or p.actif = :actif)
              and (:ressource is null or p.ressource = :ressource)
              and (:action is null or p.action = :action)
            order by p.ressource, p.action, p.designation
            """)
    List<Permission> rechercher(
            @Param("actif") Boolean actif,
            @Param("ressource") String ressource,
            @Param("action") String action);
}