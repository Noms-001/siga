package mg.bank.backend.dto.activite;

import lombok.Builder;
import lombok.Getter;

/** Agrégats calculés sur tout le résultat filtré, indépendamment des pages. */
@Getter
@Builder
public class ActiviteSuiviStatistiquesDTO {

    private final long totalActivites;
    private final long totalSousActivites;
    private final long sousActivitesTerminees;
    private final double avancementMoyen;
}