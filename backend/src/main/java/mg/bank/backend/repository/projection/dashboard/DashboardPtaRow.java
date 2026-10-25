package mg.bank.backend.repository.projection.dashboard;

public interface DashboardPtaRow {
    Long getTotal();
    Long getTerminees();
    Long getEnCours();
    Long getEnRetard();
    Long getNonCommencees();
    Double getAvancement();
}