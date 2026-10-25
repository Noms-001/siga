package mg.bank.backend.repository.projection.dashboard;

import java.time.LocalDate;

public interface DashboardPrioriteRow {
    Integer getId();
    String getCode();
    String getReference();
    String getDesignation();
    String getService();
    String getPrioriteCode();
    String getPrioriteLibelle();
    String getStatutCode();
    String getStatutLibelle();
    LocalDate getDateDebut();
    LocalDate getDateEcheance();
    Double getAvancement();
    Integer getJoursRetard();
}