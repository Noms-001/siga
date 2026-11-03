package mg.bank.backend.repository.projection.activite;

import java.time.LocalDate;

public interface SousActiviteDetailRow {

    Integer getId();

    String getCode();

    String getDesignation();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    LocalDate getDateDebutReelle();

    LocalDate getDateFinReelle();

    String getStatutActivite();

}
