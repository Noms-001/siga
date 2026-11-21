package mg.bank.backend.repository.projection.planaction;

import java.time.LocalDate;

public interface PlanActionExportRow {

    Integer getId();

    String getCode();

    String getDesignation();

    LocalDate getDateDebutPrevue();

    LocalDate getDateFinPrevue();

    LocalDate getDateDebutReelle();

    LocalDate getDateFinReelle();

    String getStatutCode();

    String getStatutLibelle();

    Double getAvancement();

    Integer getServiceId();

    String getServiceNom();

    String getServicesLies();
}
