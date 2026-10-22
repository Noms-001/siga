package mg.bank.backend.dto.importactivite;

import java.time.LocalDate;

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
public class ApercuActiviteDTO {

    /** Code fourni dans le lot, ou null s'il sera genere. */
    private String code;

    /** Reference de l'activite (obligatoire en PTA, facultative en NON_PTA). */
    private String reference;

    private String designation;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private int nombreSousActivites;
    private int nombreLivrables;

    /** "NON_COMMENCE" ou "BROUILLON", deduit de activitesDejaValidees. */
    private String statutInitial;
}