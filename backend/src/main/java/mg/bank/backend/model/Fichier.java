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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Depot de fichier sur un livrable.
 *
 * MEME JUSTIFICATION QUE LES AUTRES MAPPINGS AJOUTES ICI
 *
 * Cette table n etait lue qu en SQL natif, via une projection
 * (FichierDetailRow) : il existe un affichage et un telechargement, mais
 * personne ne deposait de fichier, donc rien n ecrivait. Le depot est
 * desormais possible depuis le detail d une sous-activite, et il se fait dans
 * la meme transaction que la creation du livrable : le mapping vaut donc
 * mieux qu un INSERT en SQL brut.
 *
 * CE QUE LA TABLE IMPOSE, ET QUI EST RESPECTE ICI
 *
 * - chemin_fichier NOT NULL : un depot sans chemin n est pas relisible, et le
 *   telechargement le lierait. La colonne est donc toujours renseignee.
 * - version NOT NULL et CHECK (version > 0) : la version commence a 1. Elle
 *   est fixee par le service de depot et non derivee d une colonne, parce
 *   qu elle compte les versions d un livrable precis, pas les lignes de la
 *   table.
 * - id_utilisateur NOT NULL : le depot est attribue a son auteur, pas a la
 *   session courante du serveur. C est la seule trace fiable de QUI a depose
 *   quoi, et l affichage s en sert.
 *
 * AUCUNE SUPPRESSION EN CASCADE
 *
 * Un depot fait partie de l historique du livrable : il ne disparait pas
 * parce qu une ligne a ete renommee ou qu un brouillon a ete reecrit. Le
 * livrable qui porte des fichiers est d ailleurs refuse a la suppression, voir
 * LivrableRepository.compterFichiers.
 */
@Entity
@Table(name = "fichier_sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fichier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fichier_sous_activite")
    private Integer idFichier;

    /**
     * VARCHAR(255) : le nom de stockage, celui ecrit sur le disque. Il n est
     * pas lisible par l utilisateur, qui ne voit que nomOriginal.
     */
    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    /**
     * VARCHAR(255) : le nom d origine, tel que le navigateur l a transmis.
     * C est celui affiche et celui du telechargement, pour que le fichier
     * retrouve le nom que l utilisateur lui connaissait.
     */
    @Column(name = "nom_original", nullable = false)
    private String nomOriginal;

    /**
     * VARCHAR(255) : chemin ABSOLU du depot sur le disque du serveur.
     *
     * Absolu et non relatif : le service qui telecharge lit cette colonne sans
     * connaitre le repertoire de travail du processus, qui n est pas fiable
     * selon le mode de lancement. Le chemin ne sort jamais en reponse.
     */
    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    /**
     * Premiere version d un livrable neve : toujours 1, la contrainte CHECK
     * l exige. Une version superieure n a de sens que pour un depot ulterieur
     * sur un livrable existant, hors du perimetre de cet ecran.
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Nullable : sans point dans le nom d origine, il n y a pas d extension a
     * deduire, et il ne faut pas en inventer une.
     */
    @Column(name = "extension")
    private String extension;

    @Column(name = "type_mime")
    private String typeMime;

    @Column(name = "taille")
    private Long taille;

    /**
     * NOT NULL en base avec une valeur par defaut. Elle est renseignee
     * explicitement plutot que laissee au defaut pour que la ligne inserree
     * soit lisible telle qu elle sera stockee, et non comme une absence de
     * choix.
     */
    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_livrable_sous_activite", nullable = false)
    private Livrable livrable;

}
