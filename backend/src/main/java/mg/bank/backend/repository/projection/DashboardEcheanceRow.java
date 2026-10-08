package mg.bank.backend.repository.projection;

import java.time.LocalDate;

public interface DashboardEcheanceRow {
    Integer getId();
    String getCode();
    String getDesignation();
    String getService();
    LocalDate getDateEcheance();
    Integer getJours();
    String getPrioriteCode();
    String getPrioriteLibelle();
    String getStatutCode();
    String getStatutLibelle();
    Double getAvancement();
}