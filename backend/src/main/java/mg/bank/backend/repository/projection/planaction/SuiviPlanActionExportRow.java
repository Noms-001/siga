package mg.bank.backend.repository.projection.planaction;

import java.time.LocalDateTime;

public interface SuiviPlanActionExportRow {

    Integer getIdPlanAction();

    LocalDateTime getDateChangement();

    String getNom();

    String getPrenom();

    Double getValeurPourcentage();

    String getCommentaire();

    String getStatutCode();

    String getStatutLibelle();
}
