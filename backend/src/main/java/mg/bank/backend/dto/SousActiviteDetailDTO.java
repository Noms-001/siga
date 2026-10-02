package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Sous-activite d une activite, avec son avancement, ses responsables et ses
 * livrables.
 *
 * avancementCourant est NULLABLE, et ce n est pas un oubli : sur les
 * donnees actuelles, seules 10 sous-activites sur 100 possedent un releve
 * dans avancement_sous_activite. Une sous-activite sans releve n a pas un
 * avancement de 0, elle n a pas d avancement constate : la nuance compte pour
 * une page de suivi, ou 0 signifie "lance a 0 pourcent" et null signifie
 * "jamais evalue".
 *
 * L ordre des sous-activites est impose par le repository et ne depend d
 * aucune valeur fournie par le client.
 */
@Getter
@Builder
@AllArgsConstructor
public class SousActiviteDetailDTO {

    private Integer id;

    private String code;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    /** Dernier releve d avancement, ou null si la sous-activite n en a pas. */
    private AvancementDetailDTO avancementCourant;

    /** Tous les releves, du plus recent au plus ancien. */
    private List<AvancementDetailDTO> historiqueAvancement;

    private List<AffectationDetailDTO> affectations;

    private List<LivrableDetailDTO> livrables;

    /**
     * Statut courant de l activite mere.
     *
     * Il n existe pas ici de statut de la sous-activite : ce qui la regit
     * est celui de son activite. Le detail l expose parce qu il decide de ce
     * qu on peut ecrire dans cette page -- un livrable ne se depose que sur
     * une activite en cours -- et que le formulaire ne doit pas avoir a le
     * redemander.
     *
     * Nullable quand l activite n a pas d historique : le formulaire traite
     * alors l absence comme un refus, ce qui est le bon defaut pour une
     * ecriture.
     */
    private String statutActivite;

}
