package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardTypeActiviteDTO {
    private Integer idTypeActivite;
    private String typeActivite;
    private long totalActivites;
    private double avancement;
}
