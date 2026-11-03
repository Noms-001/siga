package mg.bank.backend.repository.projection.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PlanActionSuiviRow {
    Integer getId();
    String getCode();
    String getDesignation();
    LocalDate getDateFinPrevue();
    String getStatutCode();
    BigDecimal getAvancement();
    Integer getResponsableId();
    String getResponsableNom();
    String getResponsablePrenom();
}
