package mg.bank.backend.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Indicateur de performance.
 *
 * Pas de date_creation / date_modification / date_desactivation en base :
 * le cycle de vie se résume à `actif`. Les valeurs mesurées vivent dans
 * valeur_indicateur (table séparée, hors périmètre de ce CRUD).
 *
 * code_hopex et indicateur_hopex sont nullable mais UNIQUE quand
 * renseignés. PostgreSQL autorise plusieurs NULL dans une colonne UNIQUE,
 * ce qui est exactement le comportement voulu : deux indicateurs peuvent
 * ne pas avoir de code HOPEx, mais deux indicateurs ne peuvent pas
 * partager le même.
 */
@Entity
@Table(name = "indicateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Indicateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_indicateur")
    private Integer idIndicateur;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "code_hopex", unique = true)
    private String codeHopex;

    @Column(name = "indicateur_hopex", unique = true)
    private String indicateurHopex;

    @Column(name = "type_indicateur")
    private String typeIndicateur;

    @Column(name = "unite_mesure")
    private String uniteMesure;

    @Column(name = "frequence_verification")
    private String frequenceVerification;

    @Column(name = "frequence_aggregation")
    private String frequenceAggregation;

    @Column(name = "definition")
    private String definition;

    @Column(name = "methode_determination")
    private String methodeDetermination;

    @Column(name = "objectif")
    private String objectif;

    @Column(name = "valeur_cible", precision = 15, scale = 2)
    private BigDecimal valeurCible;

    @Column(name = "seuil_min", precision = 15, scale = 2)
    private BigDecimal seuilMin;

    @Column(name = "seuil_max", precision = 15, scale = 2)
    private BigDecimal seuilMax;

    @Column(name = "actif")
    private Boolean actif;
}