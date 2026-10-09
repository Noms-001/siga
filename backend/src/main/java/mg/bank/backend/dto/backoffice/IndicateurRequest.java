package mg.bank.backend.dto.backoffice;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndicateurRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 250, message = "La désignation ne doit pas dépasser 250 caractères")
    private String designation;

    @Size(max = 150, message = "Le code HOPEx ne doit pas dépasser 150 caractères")
    private String codeHopex;

    @Size(max = 250, message = "L'indicateur HOPEx ne doit pas dépasser 250 caractères")
    private String indicateurHopex;

    @Size(max = 20, message = "Le type ne doit pas dépasser 20 caractères")
    private String typeIndicateur;

    @Size(max = 50, message = "L'unité de mesure ne doit pas dépasser 50 caractères")
    private String uniteMesure;

    @Size(max = 50, message = "La fréquence de vérification ne doit pas dépasser 50 caractères")
    private String frequenceVerification;

    @Size(max = 50, message = "La fréquence d'agrégation ne doit pas dépasser 50 caractères")
    private String frequenceAggregation;

    private String definition;

    private String methodeDetermination;

    private String objectif;

    private BigDecimal valeurCible;

    private BigDecimal seuilMin;

    private BigDecimal seuilMax;

    /**
     * Nullable à la création : le service applique `true` par défaut,
     * cohérent avec DEFAULT TRUE de la colonne.
     */
    private Boolean actif;

    /**
     * Vérifie la cohérence des trois bornes UNIQUEMENT quand elles sont
     * toutes renseignées.
     *
     * Un indicateur dont la cible est fixée sans seuils est valide : la
     * règle ne doit pas s'appliquer par défaut sur des champs absents,
     * sinon elle interdirait toute saisie partielle — ce qui est un cas
     * d'usage courant en cours de paramétrage.
     *
     * @AssertTrue est porté par le DTO plutôt que par le service : la règle
     * dépend uniquement du contenu de la requête, pas d'un état de la
     * base. Le service se contente de la respecter.
     */
    @AssertTrue(message = "Les seuils doivent encadrer la valeur cible : seuilMin ≤ valeurCible ≤ seuilMax")
    public boolean isCoherenceSeuils() {
        if (seuilMin == null || valeurCible == null || seuilMax == null) {
            return true;
        }
        return seuilMin.compareTo(valeurCible) <= 0
                && valeurCible.compareTo(seuilMax) <= 0;
    }
}