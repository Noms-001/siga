package mg.bank.backend.repository.projection.activite;

import java.time.LocalDateTime;

public interface AvancementSousActiviteRow {
    Integer getIdSousActivite();
    Double getValeur();
    LocalDateTime getDateChangement();
}