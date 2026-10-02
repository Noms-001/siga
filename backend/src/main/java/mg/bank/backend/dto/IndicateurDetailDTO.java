package mg.bank.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Indicateur rattache a une activite, avec son historique de mesures.
 *
 * L indicateur est defini dans indicateur et se rattache a l activite par la
 * seule table de jointure activite_indicateur, qui ne porte aucune donnee
 * propre. Les mesures ne dependent que de l indicateur : il n existe pas de
 * valeur propre a une activite, ce qui signifie qu un indicateur partage par
 * deux activites affiche les memes mesures chez les deux. C est le modele
 * existant, il n est pas interprete ici.
 *
 * L avancement de l activite ne provient PAS de ces valeurs : il vient des
 * releves de avancement_sous_activite, comme dans la liste.
 */
@Getter
@Builder
@AllArgsConstructor
public class IndicateurDetailDTO {

    private Integer id;

    private String code;

    private String codeHopex;

    private String indicateurHopex;

    private String designation;

    private String typeIndicateur;

    private String uniteMesure;

    private String frequenceVerification;

    private String frequenceAggregation;

    private String definition;

    private String methodeDetermination;

    private String objectif;

    private Double valeurCible;

    private Double seuilMin;

    private Double seuilMax;

    private Boolean actif;

    /** Mesure la plus recente, ou null si l indicateur n en a aucune. */
    private ValeurIndicateurDTO valeurCourante;

    /** Toutes les mesures, de la periode la plus recente a la plus ancienne. */
    private List<ValeurIndicateurDTO> valeurs;

}
