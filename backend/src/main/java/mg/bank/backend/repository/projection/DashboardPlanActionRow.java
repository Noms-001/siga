package mg.bank.backend.repository.projection;

public interface DashboardPlanActionRow {
    Long getTotal();
    Long getEnCours();
    Long getTermines();
    Long getEnRetard();
}