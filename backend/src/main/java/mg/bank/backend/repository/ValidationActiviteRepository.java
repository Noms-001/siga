package mg.bank.backend.repository;

import mg.bank.backend.model.ValidationActivite;
import mg.bank.backend.enums.DecisionValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

@Repository
public interface ValidationActiviteRepository
                extends JpaRepository<ValidationActivite, Integer> {

        Optional<ValidationActivite> findFirstByActivite_IdActiviteAndDecisionOrderByDateDecisionDesc(
                        Integer idActivite,
                        DecisionValidation decision);

        Optional<ValidationActivite> findFirstByActivite_IdActiviteOrderByDateDecisionDesc(
                        Integer idActivite);

        @Query("""
                        SELECT v
                        FROM ValidationActivite v
                        WHERE v.activite.idActivite = :idActivite
                        AND (v.decideur IS NULL OR v.dateDecision IS NULL)
                        ORDER BY v.etapeValidation.niveau DESC
                        """)
        Optional<ValidationActivite> findDerniereEtapeNonValidee(
                        @Param("idActivite") Integer idActivite);

        /**
         * Récupère toutes les validations DÉJÀ DÉCIDÉES pour une liste d'activités,
         * triées pour que la plus récente arrive en tête par activité.
         * On exclut l'étape en attente pour obtenir la décision "avant validation".
         */
        @Query("""
                        SELECT v FROM ValidationActivite v
                        JOIN FETCH v.etapeValidation e
                        LEFT JOIN FETCH v.decideur
                        WHERE v.activite.idActivite IN :idsActivites
                          AND v.decision <> :enAttente
                        ORDER BY v.activite.idActivite ASC, v.dateDecision DESC
                        """)
        List<ValidationActivite> findDecisionsPrecedentesByActiviteIds(
                        @Param("idsActivites") Collection<Integer> idsActivites,
                        @Param("enAttente") DecisionValidation enAttente);

        /**
         * Récupère toutes les validations EN ATTENTE pour une liste d'étapes,
         * avec l'activité et ses relations déjà chargées (évite le LAZY N+1).
         */
        @Query("""
                        SELECT v FROM ValidationActivite v
                        JOIN FETCH v.etapeValidation e
                        JOIN FETCH v.activite a
                        LEFT JOIN FETCH a.objectifSpecifique
                        LEFT JOIN FETCH a.service
                        LEFT JOIN FETCH a.typeActivite
                        LEFT JOIN FETCH a.site
                        LEFT JOIN FETCH a.priorite
                        WHERE e.idEtapeValidation IN :idsEtapes
                          AND v.decision = :enAttente
                        """)
        List<ValidationActivite> findEnAttenteByEtapes(
                        @Param("idsEtapes") Collection<Integer> idsEtapes,
                        @Param("enAttente") DecisionValidation enAttente);
}
