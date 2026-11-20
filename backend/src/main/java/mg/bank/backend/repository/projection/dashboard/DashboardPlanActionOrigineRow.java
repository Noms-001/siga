package mg.bank.backend.repository.projection.dashboard;

/**
 * Agregat d'avancement des plans d'action par origine, sur une periode.
 */
public interface DashboardPlanActionOrigineRow {
    String getOrigine();
    Long getTotal();
    Double getAvancement();
}
