package mg.bank.backend.repository.projection.activite;

import java.time.LocalDate;

public interface SousActiviteExportRow {

    Integer getIdActivite();

    Integer getIdSousActivite();

    String getCode();

    String getDesignation();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    String getStatutCode();

    String getStatutLibelle();

    Double getAvancement();
}
