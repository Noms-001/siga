package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPtaComparaisonDTO {
    private DashboardPtaDTO pta;
    private DashboardPtaDTO nonPta;
}