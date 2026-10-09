package mg.bank.backend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardKpiDTO {
    private long totalActivites;
    private long enCours;
    private long terminees;
    private long enRetard;
    private long nonCommencees;
    private long suspendues;
    private long annulees;
    private long reportees;              // ← nouveau
    private long enAttenteValidation;
    private double avancementGlobal;
}