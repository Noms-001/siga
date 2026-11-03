package mg.bank.backend.repository.projection.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IndicateurContexteRow {
    Integer getId();
    String getCode();
    String getDesignation();
    BigDecimal getValeurCourante();
    LocalDateTime getPeriodeDebut();
    LocalDateTime getPeriodeFin();
    BigDecimal getSeuilMin();
    BigDecimal getSeuilMax();
}