package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActiviteEcritureRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    @Size(max = 20, message = "La référence ne doit pas dépasser 20 caractères")
    private String reference;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

    @NotNull(message = "La date de début prévue est obligatoire")
    private LocalDate dateDebutPrevue;

    /**
     * Nullable : une activite peut etre ouverte sans echeance.
     */
    private LocalDate dateFinPrevue;

    @NotNull(message = "Le type d'activité (PTA ou non PTA) est obligatoire")
    private Boolean pta;

    /**
     * PTA = renseigne, NON PTA = absent. La coherence des deux est
     * verifiee cote service, jamais laissee a l affichage du front.
     */
    private Integer idObjectifSpecifique;

    private Integer idTypeActivite;

    private Integer idSite;

    @NotNull(message = "La priorité est obligatoire")
    private Integer idPriorite;

    private Integer idService;

    @Valid
    private List<SousActiviteEcritureRequest> sousActivites = new ArrayList<>();

    @Valid
    private List<ResultatIntermediaireEcritureRequest> resultatsIntermediaires
            = new ArrayList<>();

}
