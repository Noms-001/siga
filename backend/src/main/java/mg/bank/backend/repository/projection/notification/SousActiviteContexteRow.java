package mg.bank.backend.repository.projection.notification;

import java.time.LocalDate;

public interface SousActiviteContexteRow {
    Integer getId();
    Integer getIdActivite();
    String getCode();
    String getDesignation();
    LocalDate getDateFinPrevue();
    String getStatutCode();
    String getStatutLibelle();
    Double getAvancement();
    String getResponsablePrenom();
    String getResponsableNom();
}