package mg.bank.backend.repository.projection;

public interface DashboardAvancementRow {
    Long getTotalActivites();
    Long getTotalSousActivites();
    Long getSousActivitesTerminees();
    Double getAvancementGlobal();
}