package mg.bank.backend.repository.projection.activite;

public interface ActiviteSuiviStatistiquesRow {

    Long getTotalActivites();

    Long getTotalSousActivites();

    Long getSousActivitesTerminees();

    Double getAvancementMoyen();
}