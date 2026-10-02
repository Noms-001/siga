package mg.bank.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Livrable attendu d une sous-activite.
 *
 * MEME JUSTIFICATION QUE SousActivite ET ResultatIntermediaire
 *
 * Cette table n etait lue qu en SQL natif, via une projection
 * (LivrableDetailRow). Aucun mapping JPA n existait, parce que rien ne
 * l ecrivait. Le formulaire de redaction devant pouvoir declarer les livrables
 * d une sous-activite dans la meme transaction que l activite mere, le mapping
 * est ajoute ici plutot qu un INSERT en SQL brut.
 *
 * CONTRAIREMENT AUX DEUX AUTRES, LA SUPPRESSION EST CONTROLEE
 *
 * fichier_sous_activite reference livrable_sous_activite : un livrable porte
 * les depots de fichiers, et supprimer la ligne les laisserait orphelins. Un
 * livrable qui porte des fichiers est donc refuse a la suppression, comme
 * une sous-activite qui porte un livrable. Voir
 * LivrableRepository.compterDependances, et le meme garde deja present sur
 * SousActiviteRepository pour les lignes qui portent un livrable.
 *
 * Les FICHIERS NE SONT PAS ECRITS ICI
 *
 * Le depot et le telechargement relevent du suivi, sur une page dediee. La
 * colonne description est un texte libre de cadrage, pas le contenu d un
 * fichier : l exposer dans le mapping n inventerait pas de donnee, mais le
 * remplir par formulaire non plus.
 *
 * CE QUI N EST PAS DANS LE MODELE
 *
 * Ni quantite, ni unite, ni echeance, ni statut de reception : la table ne
 * porte que ces quatre colonnes.
 */
@Entity
@Table(name = "livrable_sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_livrable_sous_activite")
    private Integer idLivrable;

    /**
     * VARCHAR(250) en base, d ou le meme plafond sur le DTO d ecriture.
     */
    @Column(name = "designation", nullable = false)
    private String designation;

    /**
     * Nullable : la description cadre le livrable, elle ne le definit pas.
     */
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sous_activite", nullable = false)
    private SousActivite sousActivite;

}
