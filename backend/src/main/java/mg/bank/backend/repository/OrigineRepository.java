package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Origine;
import mg.bank.backend.repository.projection.PlanActionOrigineRow;

public interface OrigineRepository extends JpaRepository<Origine, Integer> {

     boolean existsByCode(String code);

     
     @Query(value = """
               SELECT o.* FROM origine o
               WHERE (CAST(:typesCsv AS TEXT) IS NULL
                      OR o.type_origine = ANY(string_to_array(CAST(:typesCsv AS TEXT), ',')))
                 AND (CAST(:motif AS TEXT) IS NULL
                      OR o.code ILIKE CAST(:motif AS TEXT)
                      OR o.designation ILIKE CAST(:motif AS TEXT))
                 AND (CAST(:avecPlan AS BOOLEAN) IS NULL
                      OR CAST(:avecPlan AS BOOLEAN) = EXISTS (
                          SELECT 1 FROM plan_action_origine pao
                          WHERE pao.id_origine = o.id_origine
                      ))
                 AND (CAST(:annee AS INTEGER) IS NULL
                      OR EXTRACT(YEAR FROM o.date_creation) = CAST(:annee AS INTEGER))
               ORDER BY o.date_creation DESC, o.id_origine DESC
               """, nativeQuery = true)
     List<Origine> rechercher(
               @Param("typesCsv") String typesCsv,
               @Param("motif") String motif,
               @Param("avecPlan") Boolean avecPlan,
               @Param("annee") Integer annee);

     @Query(value = "SELECT DISTINCT type_origine FROM origine ORDER BY type_origine", nativeQuery = true)
     List<String> findDistinctTypes();

     /**
      * Plans d'action rattachés à un signalement.
      *
      * Relation M2M via plan_action_origine : un signalement peut être lié à
      * plusieurs plans. Tri par est_principale DESC d'abord — le plan
      * marqué principal remonte en tête — puis par code.
      *
      * Le NULLS LAST est défensif : est_principale est nullable en base (la
      * contrainte n'impose rien, elle interdit seulement deux principales
      * pour le même plan). Un signalement normal n'a qu'une seule ligne
      * principale, mais plusieurs lignes sans marque restent possibles.
      */
     @Query(value = """
               SELECT pa.id_plan_action  AS id,
                      pa.code            AS code,
                      pa.designation     AS designation,
                      pao.est_principale AS estPrincipale
               FROM plan_action_origine pao
               JOIN plan_action pa ON pa.id_plan_action = pao.id_plan_action
               WHERE pao.id_origine = :idOrigine
               ORDER BY pao.est_principale DESC NULLS LAST, pa.code
               """, nativeQuery = true)
     List<PlanActionOrigineRow> findPlansActionByOrigine(@Param("idOrigine") Integer idOrigine);
}