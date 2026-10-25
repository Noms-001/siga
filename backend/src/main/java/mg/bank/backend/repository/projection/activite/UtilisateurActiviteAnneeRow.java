package mg.bank.backend.repository.projection.activite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UtilisateurActiviteAnneeRow {
    Integer getId();
    String getCode();
    String getDesignation();
    String getReference();

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