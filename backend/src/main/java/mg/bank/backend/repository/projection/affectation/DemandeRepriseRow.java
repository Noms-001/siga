package mg.bank.backend.repository.projection.affectation;

import java.time.LocalDateTime;

public interface DemandeRepriseRow {
    Integer getId();
    LocalDateTime getDateDemande();
    LocalDateTime getDateDecision();
    String getStatut();
    String getMotif();
    String getCommentaireDecision();

    Integer getActiviteId();
    String getActiviteCode();
    String getActiviteDesignation();

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