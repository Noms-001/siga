package mg.bank.backend.repository.projection.dashboard;

public interface DashboardPlanActionRow {
    Long getTotal();
    Long getEnCours();
    Long getTermines();
    Long getEnRetard();
}