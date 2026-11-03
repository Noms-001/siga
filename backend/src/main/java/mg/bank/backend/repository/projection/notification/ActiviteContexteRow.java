package mg.bank.backend.repository.projection.notification;

import java.time.LocalDate;

public interface ActiviteContexteRow {
    Integer getId();
    String getCode();
    String getDesignation();
    LocalDate getDateFinPrevue();
    String getStatutCode();
    String getStatutLibelle();
    Double getAvancement();
}