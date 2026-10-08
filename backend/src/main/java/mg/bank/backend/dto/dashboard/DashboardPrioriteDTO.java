package mg.bank.backend.dto.dashboard;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPrioriteDTO {
    private Integer id;
    private String code;
    private String reference;
    private String designation;
    private String service;
    private String priorite;
    private String prioriteCode;
    private String statut;
    private String statutLibelle;
    private LocalDate dateDebut;
    private LocalDate dateEcheance;
    private double avancement;
    private long joursRetard;
}