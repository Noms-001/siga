package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

/**
 * Avancement moyen des plans d'action pour une origine, sur la periode
 * globale selectionnee.
 *
 * `avancement` est la moyenne des pourcentages saisis sur les plans de
 * l'origine (0 pour un plan sans avancement renseigne). `total` est le
 * nombre de plans de l'origine qui chevauchent la periode.
 */
@Getter
@Builder
public class DashboardPlanActionOrigineDTO {

    private String origine;
    private String libelle;
    private long total;
    private double avancement;
}
