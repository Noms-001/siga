package mg.bank.backend.repository.projection.planaction;

import java.time.LocalDateTime;

public interface MotifReprisePlanExportRow {

    Integer getIdPlanAction();

    LocalDateTime getDateDemande();

    String getMotif();
}
