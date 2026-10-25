package mg.bank.backend.repository.affectation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mg.bank.backend.model.affectation.AffectationSousActivite;
import mg.bank.backend.repository.projection.affectation.AffectationRow;

@Repository
public interface AffectationSousActiviteRepository
        extends JpaRepository<AffectationSousActivite, Integer> {

    @Query(value = """
            SELECT aa.id_affectation_sous_activite AS id,
                   aa.date_affectation             AS dateAffectation,
                   aa.date_desaffectation          AS dateDesaffectation,
                   r.id_role                       AS roleId,
                   r.code                          AS roleCode,
                   r.designation                   AS roleLibelle,
                   u.id_utilisateur                AS utilisateurId,
                   u.nom                           AS utilisateurNom,
                   u.prenom                        AS utilisateurPrenom
            FROM affectation_sous_activite aa
            JOIN role r        ON r.id_role = aa.id_role
            JOIN utilisateur u ON u.id_utilisateur = aa.id_utilisateur
            WHERE aa.id_sous_activite = :idSousActivite
            ORDER BY aa.date_affectation DESC, aa.id_affectation_sous_activite DESC
            """, nativeQuery = true)
    List<AffectationRow> findHistorique(@Param("idSousActivite") Integer idSousActivite);

    @Query(value = """
            SELECT COUNT(*) FROM affectation_sous_activite
            WHERE id_sous_activite = :idSousActivite
              AND id_role = :idRole
              AND date_desaffectation IS NULL
            """, nativeQuery = true)
    long countActivesPourRole(@Param("idSousActivite") Integer idSousActivite,
                              @Param("idRole") Integer idRole);

    @Query(value = """
            SELECT COUNT(*) FROM affectation_sous_activite aa
            JOIN role r ON r.id_role = aa.id_role
            WHERE aa.id_sous_activite = :idSousActivite
              AND r.code = 'RESPONSABLE'
              AND aa.date_desaffectation IS NULL
            """, nativeQuery = true)
    long countResponsableActif(@Param("idSousActivite") Integer idSousActivite);

    @Query(value = """
            SELECT COUNT(*) FROM affectation_sous_activite
            WHERE id_affectation_sous_activite = :idAffectation
              AND id_sous_activite = :idSousActivite
            """, nativeQuery = true)
    long countAppartenance(@Param("idSousActivite") Integer idSousActivite,
                           @Param("idAffectation") Integer idAffectation);

    Optional<AffectationSousActivite> findByIdAffectationSousActiviteAndSousActiviteIdSousActivite(
            Integer idAffectation, Integer idSousActivite);
}