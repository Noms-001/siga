package mg.bank.backend.dto.indicateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValeurIndicateurDTO {
    private Integer id;
    private BigDecimal valeur;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;
    private String commentaire;
    private LocalDateTime dateSaisie;
    private UtilisateurMinimalDTO saisiePar;
}