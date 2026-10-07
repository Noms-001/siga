package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

public interface HistoriqueAvancementRow {
    Integer getId();
    Double getValeur();
    String getCommentaire();
    LocalDateTime getDateChangement();
    String getStatutCode();
    String getStatutLibelle();
    Integer getUtilisateurId();
    String getUtilisateurNom();
    String getUtilisateurPrenom();
}