package mg.bank.backend.dto.dashboard;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardEcheancesDTO {
    private List<DashboardEcheanceDTO> enRetard;
    private List<DashboardEcheanceDTO> echeanceProche;
}