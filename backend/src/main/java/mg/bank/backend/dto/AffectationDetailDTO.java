package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Affectation d un utilisateur a une sous-activite, dans un role.
 *
 * dateDesaffectation distingue une affectation en cours d une affectation
 * close : le role n est pas efface, il est borne dans le temps. Sans cette
 * colonne, l historique des responsables serait perdu.
 */
@Getter
@Builder
@AllArgsConstructor
public class AffectationDetailDTO {

    private Integer id;

    private LocalDateTime dateAffectation;

    /** Nulle tant que l utilisateur est affecte. */
    private LocalDateTime dateDesaffectation;

    private ReferenceDTO role;

    private UtilisateurResumeDTO utilisateur;

    /** Vrai si dateDesaffectation est nulle. */
    public boolean estActive() {
        return dateDesaffectation == null;
    }

}
