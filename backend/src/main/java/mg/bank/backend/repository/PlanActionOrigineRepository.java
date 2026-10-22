package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.PlanActionOrigine;
import mg.bank.backend.repository.projection.PlanActionOrigineDetailRow;

public interface PlanActionOrigineRepository
        extends JpaRepository<PlanActionOrigine, PlanActionOrigine.Cle> {

    @Query(value = """
            SELECT o.id_origine  AS idOrigine,
                   o.code        AS code,
                   o.designation AS designation,
                   o.type_origine AS typeOrigine,
                   pao.est_principale AS estPrincipale
            FROM plan_action_origine pao
            JOIN origine o ON o.id_origine = pao.id_origine
            WHERE pao.id_plan_action = :idPlanAction
            ORDER BY pao.est_principale DESC NULLS LAST, o.code
            """, nativeQuery = true)
    List<PlanActionOrigineDetailRow> findByPlanAction(@Param("idPlanAction") Integer idPlanAction);

    void deleteByIdPlanAction(Integer idPlanAction);

    boolean existsByIdPlanActionAndIdOrigine(Integer idPlanAction, Integer idOrigine);
}