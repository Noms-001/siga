package mg.bank.backend.dto.activite;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.common.UtilisateurResumeDTO;


@Getter
@Builder
@AllArgsConstructor
public class AvancementDetailDTO {

    private Integer id;

    /** Pourcentage, de 0 a 100. */
    private Double valeurPourcentage;

    private String commentaire;

    private LocalDateTime dateChangement;

    private ReferenceDTO statut;

    private UtilisateurResumeDTO utilisateur;

    /** Vrai pour le seul releve retenu comme avancement courant. */
    private boolean etatActuel;

}
