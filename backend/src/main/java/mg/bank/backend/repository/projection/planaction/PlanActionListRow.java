// PlanActionListRow.java
package mg.bank.backend.repository.projection.planaction;

import java.time.LocalDate;

public interface PlanActionListRow {
    Integer getId();
    String getCode();
    String getDesignation();
    LocalDate getDateDebutPrevue();
    LocalDate getDateFinPrevue();
    LocalDate getDateDebutReelle();
    LocalDate getDateFinReelle();
    Integer getStatutId();
    String getStatutCode();
    String getStatutLibelle();
    java.math.BigDecimal getAvancement();
    Integer getResponsableId();
    String getResponsableNom();
    String getResponsablePrenom();
}