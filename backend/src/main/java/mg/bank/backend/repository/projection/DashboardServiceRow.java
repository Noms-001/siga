package mg.bank.backend.repository.projection;

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