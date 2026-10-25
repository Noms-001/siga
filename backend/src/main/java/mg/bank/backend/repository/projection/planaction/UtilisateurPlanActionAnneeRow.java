package mg.bank.backend.repository.projection.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UtilisateurPlanActionAnneeRow {
    Integer getId();
    String getCode();
    String getDesignation();

    LocalDate getDateDebutPrevue();
    LocalDate getDateFinPrevue();
    LocalDate getDateDebutReelle();
    LocalDate getDateFinReelle();

    LocalDateTime getDateAffectation();
    LocalDateTime getDateDesaffectation();

    String getRoleCode();
    String getRoleLibelle();

    String getStatutCode();
    String getStatutLibelle();

    BigDecimal getAvancement();
}