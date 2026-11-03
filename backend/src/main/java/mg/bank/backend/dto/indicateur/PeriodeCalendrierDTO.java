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
public class PeriodeCalendrierDTO {

    private String libelle;
    private LocalDateTime debut;
    private LocalDateTime fin;

    /** Null si aucune valeur saisie. */
    private Integer valeurId;
    private BigDecimal valeur;
    private String commentaire;
    private LocalDateTime dateSaisie;
    private String saisieParLibelle;

    /**
     * ATTEINT | NON_ATTEINT | INDETERMINE | NON_SAISI | EN_COURS | MANQUANT
     *
     * - ATTEINT / NON_ATTEINT : valeur presente, statut KPI/KRI calcule
     * - INDETERMINE : valeur presente mais cible ou type absents
     * - NON_SAISI : aucune valeur, periode future
     * - EN_COURS : aucune valeur, periode actuelle en cours
     * - MANQUANT : aucune valeur, periode passee (alerte de saisie)
     */
    private String statut;

    /** La periode contient aujourd'hui. */
    private boolean estEnCours;

    /** La periode est entierement dans le futur. */
    private boolean estFuture;

    /** La periode est entierement dans le passe. */
    private boolean estPassee;
}