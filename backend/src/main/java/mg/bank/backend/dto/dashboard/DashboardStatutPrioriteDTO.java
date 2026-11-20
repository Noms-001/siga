package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatutPrioriteDTO {
    private String statut;
    private String statutLibelle;
    private String priorite;
    private String prioriteLibelle;
    private long nombre;
}
