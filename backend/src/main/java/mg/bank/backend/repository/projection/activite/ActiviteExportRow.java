package mg.bank.backend.repository.projection.activite;

import java.time.LocalDate;

public interface ActiviteExportRow {

    Integer getId();

    String getCode();

    String getReference();

    String getDesignation();

    Boolean getPta();

    String getObjectifCode();

    String getObjectifDesignation();

    String getProcedureLibelle();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    String getStatutCode();

    String getStatutLibelle();

    Integer getServiceId();

    String getServiceNom();

    String getPrioriteLibelle();

    Double getAvancement();
}
