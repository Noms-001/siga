package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

public interface DashboardValidationRow {
    Integer getIdValidation();
    Integer getIdActivite();
    String getCodeActivite();
    String getDesignationActivite();
    String getDemandeurNom();
    String getDemandeurPrenom();
    String getEtapeDesignation();
    Integer getEtapeNiveau();
    LocalDateTime getDateDemande();
    String getDecision();
}