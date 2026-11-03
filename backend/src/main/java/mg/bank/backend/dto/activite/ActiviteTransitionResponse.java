package mg.bank.backend.dto.activite;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActiviteTransitionResponse {

    private Integer idActivite;

    private String code;

    private String designation;

    private String statutCode;

    private String statutLibelle;

    /** Renseignee au demarrage (et reprise telle quelle a la cloture). */
    private LocalDate dateDebutReelle;

    /** Renseignee a la cloture. */
    private LocalDate dateFinReelle;

    /** Horodatage de la transition, celui qui figure dans l'historique. */
    private LocalDateTime dateTransition;
}