package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Mesure d un indicateur sur une periode.
 *
 * Comme avancement_sous_activite, valeur_indicateur est un historique : une
 * ligne par periode de suivi. periodeDebut et periodeFin bornent la mesure,
 * et la contrainte chk_valeur_indicateur_periode impose periodeFin >
 * periodeDebut, ce qui rend un ordre par periodeFin coherent.
 */
@Getter
@Builder
@AllArgsConstructor
public class ValeurIndicateurDTO {

    private Integer id;

    private Double valeur;

    private LocalDateTime periodeDebut;

    private LocalDateTime periodeFin;

    private String commentaire;

    private LocalDateTime dateSaisie;

    private UtilisateurResumeDTO utilisateur;

    /** Vrai pour la seule valeur retenue comme valeur courante. */
    private boolean valeurCourante;

}
