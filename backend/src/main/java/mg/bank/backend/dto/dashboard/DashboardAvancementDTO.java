package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardAvancementDTO {
    private double avancementGlobal;
    private long totalActivites;
    private long totalSousActivites;
    private long sousActivitesTerminees;
}