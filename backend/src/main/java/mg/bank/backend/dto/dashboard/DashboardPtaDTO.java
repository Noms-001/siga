package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPtaDTO {
    private long total;
    private long terminees;
    private long enCours;
    private long enRetard;
    private long nonCommencees;
    private double avancement;
}