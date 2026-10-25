package mg.bank.backend.repository.projection.dashboard;

public interface DashboardAvancementRow {
    Long getTotalActivites();
    Long getTotalSousActivites();
    Long getSousActivitesTerminees();
    Double getAvancementGlobal();
}