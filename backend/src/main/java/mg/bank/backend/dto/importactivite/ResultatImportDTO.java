package mg.bank.backend.dto.importactivite;

import java.util.List;

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
public class ResultatImportDTO {

    private String message;

    private int nombreActivitesImportees;
    private int nombreSousActivitesImportees;
    private int nombreLivrablesImportes;
    private int nombreResultatsIntermediairesImportes;

    /**
     * Codes d'activites et de sous-activites qui ont ete generes par le
     * backend parce qu'ils etaient absents du JSON.
     */
    private List<String> codesGeneres;

    /** Codes d'indicateurs qui ont ete crees automatiquement. */
    private List<String> indicateursCrees;

    /** Codes d'origines qui ont ete creees automatiquement. */
    private List<String> originesCreees;

    /**
     * Statut initial attribue a toutes les activites du lot :
     * {@code NON_COMMENCE} ou {@code BROUILLON}.
     */
    private String statutInitial;
}