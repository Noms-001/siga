package mg.bank.backend.repository.projection.dashboard;

public interface DashboardTypeActiviteRow {
    Integer getIdTypeActivite();
    String getTypeActivite();
    Long getTotalActivites();
    Double getAvancement();
}
