package mg.bank.backend.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import mg.bank.backend.model.EtapeValidation;

@Repository
public interface EtapeValidationRepository
                extends JpaRepository<EtapeValidation, Integer> {

        /**
         * Récupère l'étape immédiatement précédente
         * dans la même procédure.
         */
        Optional<EtapeValidation> findFirstByProcedure_IdProcedureAndNiveauLessThanOrderByNiveauDesc(
                        Integer idProcedure,
                        Integer niveau);

        /**
         * Récupère l'étape immédiatement suivante
         * dans la même procédure.
         */
        Optional<EtapeValidation> findFirstByProcedure_IdProcedureAndNiveauGreaterThanOrderByNiveauAsc(
                        Integer idProcedure,
                        Integer niveau);

        /**
         * Récupère la première étape active
         * dans la même procédure.
         */
        Optional<EtapeValidation> findFirstByProcedure_IdProcedureAndActifTrueAndPostesDecideurs_IdPosteOrderByNiveauAsc(
                        Integer idProcedure,
                        Integer idPoste);

        Optional<EtapeValidation> findFirstByProcedure_IdProcedureAndActifTrueOrderByNiveauAsc(
                        Integer idProcedure);

        Optional<EtapeValidation> findFirstByProcedure_IdProcedureAndActifTrueAndNiveauGreaterThanOrderByNiveauAsc(
                        Integer idProcedure,
                        Integer niveau);

        List<EtapeValidation> findByPostesDecideurs_IdPosteAndActifTrueOrderByNiveauAsc(
                        Integer idPoste);

        @Query("""
                        select distinct e from EtapeValidation e
                        left join fetch e.postesDecideurs
                        where e.procedure.idProcedure = :idProcedure
                        order by e.niveau asc
                        """)
        List<EtapeValidation> findByProcedureWithDecideurs(@Param("idProcedure") Integer idProcedure);
}