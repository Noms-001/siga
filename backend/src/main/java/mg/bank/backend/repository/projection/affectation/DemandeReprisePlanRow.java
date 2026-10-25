package mg.bank.backend.repository.projection.affectation;

import java.time.LocalDateTime;

public interface DemandeReprisePlanRow {
    Integer getId();
    LocalDateTime getDateDemande();
    LocalDateTime getDateDecision();
    String getStatut();
    String getMotif();
    String getCommentaireDecision();

    Integer getPlanActionId();
    String getPlanActionCode();
    String getPlanActionDesignation();

    Integer getDemandeurId();
    String getDemandeurNom();
    String getDemandeurPrenom();

    Integer getResponsableId();
    String getResponsableNom();
    String getResponsablePrenom();

    Integer getDecideurId();
    String getDecideurNom();
    String getDecideurPrenom();
}