package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

public interface AvancementSousActiviteRow {
    Integer getIdSousActivite();
    Double getValeur();
    LocalDateTime getDateChangement();
}