package mg.bank.backend.repository.projection.indicateur;

import java.math.BigDecimal;

/**
 * Projection d un indicateur rattache a une activite.
 *
 * Les seuils et la valeur cible sont des numeric(15,2) : ils sont exposes en
 * Double comme le pourcentage d avancement, plutot qu en BigDecimal, pour ne
 * pas imposer au front une notion de precision decimale sur des grandeurs qui
 * ne servent qu a l affichage.
 */
public interface IndicateurDetailRow {

    Integer getId();

    String getCode();

    String getCodeHopex();

    String getIndicateurHopex();

    String getDesignation();

    String getTypeIndicateur();

    String getUniteMesure();

    String getFrequenceVerification();

    String getFrequenceAggregation();

    String getDefinition();

    String getMethodeDetermination();

    String getObjectif();

    BigDecimal getValeurCible();

    BigDecimal getSeuilMin();

    BigDecimal getSeuilMax();

    Boolean getActif();

}
