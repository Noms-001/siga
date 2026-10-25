package mg.bank.backend.repository.projection.affectation;

import java.time.LocalDateTime;

public interface AffectationRow {
    Integer getId();
    LocalDateTime getDateAffectation();
    LocalDateTime getDateDesaffectation();
    Integer getRoleId();
    String getRoleCode();
    String getRoleLibelle();
    Integer getUtilisateurId();
    String getUtilisateurNom();
    String getUtilisateurPrenom();
}