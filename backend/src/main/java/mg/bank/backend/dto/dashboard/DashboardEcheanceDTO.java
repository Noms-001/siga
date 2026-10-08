package mg.bank.backend.dto.dashboard;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardEcheanceDTO {
    private Integer id;
    private String code;
    private String designation;
    private String service;
    private LocalDate dateEcheance;
    private long jours;              // négatif si en retard
    private String priorite;
    private String prioriteCode;
    private String statut;
    private String statutLibelle;
    private double avancement;
}