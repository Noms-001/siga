package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Accuse de soumission d une activite.
 *
 * Renvoie l activite et le statut obtenu, pas l identifiant de la ligne
 * d historique : ce dernier n a aucun usage cote front, qui veut savoir que
 * la transition a eu lieu, et relira l historique par le detail si besoin.
 *
 * statut est un code et non un objet ReferenceDTO : ReferenceDTO porte un
 * identifiant de base et un libelle, alors qu ici le code suffit et que le
 * libelle serait une donnee dupliquee. Le front sait deja traduire un code
 * de statut, il le fait pour la liste.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActiviteSoumissionDTO {

    private Integer idActivite;

    private String code;

    /**
     * Statut atteint, toujours EN_ATTENTE_VALIDATION. Constant par
     * construction, mais renvoye pour que le client n ait pas a le deviner.
     */
    private String statut;

    private LocalDateTime dateSoumission;

}
