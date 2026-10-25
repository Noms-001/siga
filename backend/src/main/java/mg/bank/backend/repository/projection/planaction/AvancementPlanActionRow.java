// AvancementPlanActionRow.java
package mg.bank.backend.repository.projection.planaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AvancementPlanActionRow {
    Integer getId();
    BigDecimal getValeurPourcentage();
    String getCommentaire();
    LocalDateTime getDateChangement();
    Integer getStatutId();
    String getStatutCode();
    String getStatutLibelle();
    Integer getUtilisateurId();
    String getUtilisateurNom();
    String getUtilisateurPrenom();
}