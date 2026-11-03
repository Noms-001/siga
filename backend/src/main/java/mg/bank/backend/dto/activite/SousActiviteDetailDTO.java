package mg.bank.backend.dto.activite;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.dto.affectation.AffectationDetailDTO;

@Getter
@Builder
@AllArgsConstructor
public class SousActiviteDetailDTO {

    private Integer id;

    private String code;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    private AvancementDetailDTO avancementCourant;

    private List<AvancementDetailDTO> historiqueAvancement;

    private List<AffectationDetailDTO> affectations;

    private List<LivrableDetailDTO> livrables;

    private String statutActivite;

    private boolean peutSaisirAvancement;

}
