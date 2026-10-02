package mg.bank.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Decision de validation prise sur une activite soumise.
 *
 * CE QUE LE CORPS CONTIENT
 * La decision, et le commentaire qui l'accompagne. Il ne contient ni
 * identifiant ni statut : l'activite vient du chemin, et le statut est une
 * consequence de la decision, jamais une donnee choisie par le client.
 *
 * LA DECISION EST UN NOM, PAS UN STATUT
 * Le client envoie VALIDE, REJETE ou RETOUR_MODIFICATION -- les trois issues
 * possibles d'un examen. Il n'envoie pas VALIDEE ou BROUILLON, qui sont des
 * statuts de suivi : accepter un statut en corps permettrait de soumettre une
 * activite dans un etat que le responsable n'a pas choisi.
 *
 * LE COMMENTAIRE EST IMPOSE POUR UN REFUS
 * Valider ne demande rien a dire. Rejeter, oui : l auteur doit pouvoir savoir
 * quoi corriger, et un refus sans motif ne lui laisse que la possibilite de
 *deviner. La contrainte est donc dans le service, qui doit distinguer les deux
 * cas -- une seule annotation NotBlank rendrait la regle moins lisible.
 */
@Getter
@Setter
public class ActiviteDecisionRequest {

    @NotNull(message = "La décision est obligatoire")
    private String decision;

    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
    private String commentaire;

    public String commentaireNettoye() {
        return commentaire == null ? null : commentaire.trim();
    }

    public boolean commentaireObligatoire() {
        return !"VALIDE".equals(decision);
    }

    /**
     * Regle de coherence annoncee au client avant l appel.
     *
     * Elle vit dans le DTO et non dans le controleur pour que le message et la
     * regle qui le produit ne puissent pas diverger.
     */
}
