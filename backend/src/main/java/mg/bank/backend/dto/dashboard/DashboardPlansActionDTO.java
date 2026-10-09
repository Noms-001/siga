package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPlansActionDTO {
    private long total;
    private long enCours;
    private long termines;
    private long enRetard;
}