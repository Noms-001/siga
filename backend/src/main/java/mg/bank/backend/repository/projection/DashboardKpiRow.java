package mg.bank.backend.repository.projection;

public interface DashboardKpiRow {
    Long getTotal();
    Long getEnCours();
    Long getTerminees();
    Long getEnRetard();
    Long getNonCommencees();
    Long getSuspendues();
    Long getAnnulees();
    Long getReportees(); 
    Long getEnAttenteValidation();
    Double getAvancementGlobal();
}