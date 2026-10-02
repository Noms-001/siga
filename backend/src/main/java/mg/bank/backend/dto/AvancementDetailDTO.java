package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Releve d avancement d une sous-activite.
 *
 * avancement_sous_activite EST un historique : une meme sous-activite y
 * accumule une ligne par releve. L avancement retenu par l activite est celui
 * de la derniere ligne, celle dont le couple
 * (date_changement, id_historique_sous_activite) est maximal, exactement comme
 * le statut courant d une activite est celui de son dernier historique.
 *
 * Cette DTO sert donc aux deux besoins : la ligne marquee etatActuel est
 * l avancement courant d une sous-activite, les autres forment son suivi.
 */
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
