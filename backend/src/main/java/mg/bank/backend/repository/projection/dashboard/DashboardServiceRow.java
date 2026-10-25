package mg.bank.backend.repository.projection.dashboard;

public interface DashboardServiceRow {
    Integer getIdService();
    String getService();
    Long getTotalActivites();
    Long getTerminees();
    Long getEnCours();
    Long getEnRetard();
    Long getNonCommencees();
    Double getAvancement();
}