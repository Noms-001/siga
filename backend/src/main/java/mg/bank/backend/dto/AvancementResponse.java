package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvancementResponse {
    private Integer idSousActivite;

    /**
     * Valeur avant l'enregistrement. Null si premier relevé.
     * Utile au front pour afficher "75% → 80%" dans la confirmation
     * ou un futur toast.
     */
    private Double ancienneValeur;

    private Double avancement;
    private String commentaire;
    private LocalDateTime dateChangement;
    private String statutCode;
    private String statutLibelle;

    /** Avancement recalculé de l'activité parente. */
    private Double avancementActivite;
}