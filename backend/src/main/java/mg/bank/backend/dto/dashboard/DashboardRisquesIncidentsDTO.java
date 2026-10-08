package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

/**
 * Conteneur des KPI risques / incidents / plans d'action.
 *
 * Le modèle SAGA ne porte à ce jour QUE plan_action : aucun risque ni
 * incident n'a de table. Les deux champs correspondants sont donc absents
 * du DTO — un champ null serait un mensonge silencieux (le front ne
 * pourrait pas distinguer "aucun incident" de "pas de module incidents").
 * À ajouter quand les tables existeront.
 */
@Getter
@Builder
public class DashboardRisquesIncidentsDTO {
    private DashboardPlansActionDTO plansAction;
}