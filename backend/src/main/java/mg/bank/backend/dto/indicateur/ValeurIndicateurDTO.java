package mg.bank.backend.dto.indicateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.dto.common.UtilisateurResumeDTO;


@Getter
@Builder
@AllArgsConstructor
public class ValeurIndicateurDTO {

    private Integer id;

    private BigDecimal valeur;

    private LocalDateTime periodeDebut;

    private LocalDateTime periodeFin;

    private String commentaire;

    private LocalDateTime dateSaisie;

    private UtilisateurResumeDTO utilisateur;

    private boolean valeurCourante;

}
