package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatutDTO {
    private String statut;
    private String libelle;
    private long nombre;
}