package mg.bank.backend.model.indicateur;

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

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "designation", nullable = false, length = 250)
    private String designation;

    @Column(name = "code_hopex", unique = true, length = 150)
    private String codeHopex;

    @Column(name = "indicateur_hopex", unique = true, length = 250)
    private String indicateurHopex;

    @Column(name = "type_indicateur", length = 20)
    private String typeIndicateur;

    @Column(name = "unite_mesure", length = 50)
    private String uniteMesure;

    @Column(name = "frequence_verification", length = 50)
    private String frequenceVerification;

    @Column(name = "frequence_aggregation", length = 50)
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