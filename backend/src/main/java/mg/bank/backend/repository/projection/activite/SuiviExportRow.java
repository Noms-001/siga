package mg.bank.backend.repository.projection.activite;

import java.time.LocalDateTime;

public interface SuiviExportRow {

    Integer getIdActivite();

    Integer getIdSousActivite();

    String getCodeSousActivite();

    String getDesignationSousActivite();

    LocalDateTime getDateChangement();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

    Double getValeurPourcentage();

    String getCommentaire();

    String getMotif();

    String getStatutCode();

    String getStatutLibelle();
}
