package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Livrable;

public interface LivrableRepository extends JpaRepository<Livrable, Integer> {

    /**
     * Livrables d'une sous-activite, dans l'ordre de saisie.
     *
     * Le tri par identifiant n'est pas un gout : c'est ce qui permet a la
     * synchronisation de comparer les lignes renvoyees par le formulaire a
     * celles qui existent, ligne a ligne, dans l'ordre de saisie. Un tri par
     * designation les melangerait et aucune ligne ne serait reconnue.
     *
     * La methode derivee suffit : il n'y a rien a ecrire qui ne soit deja le
     * nom de la propriete et de l'ordre.
     */
    List<Livrable> findBySousActiviteIdSousActiviteOrderByIdLivrableAsc(
            Integer idSousActivite);

    /**
     * Un livrable porte deja cette designation sur cette sous-activite ?
     *
     * La base n impose rien -- rien n est UNIQUE sur (sous-activite,
     * designation) -- mais deux livrables de meme libelle sur la meme
     * sous-activite sont presque toujours une double validation du meme
     * depot. Les laisser tous deux afficherait deux lignes identiques, sans
     * que rien ne distingue laquelle est la bonne.
     *
     * Comparaison insensible a la casse : "Rapport final" et "rapport FINAL"
     * sont le meme livrable pour un lecteur, et le laisser passer produirait
     * exactement le doublon qu on veut eviter. Les espaces, eux, sont
     * normalises en amont : le service interroge toujours une designation
     * recaptee, donc un libelle saisi avec un espace final n arrive pas ici
     * comme un cas distinct.
     */
    boolean existsBySousActiviteIdSousActiviteAndDesignationIgnoreCase(
            Integer idSousActivite, String designation);


    /**
     * Un livrable porte-t-il encore des fichiers deposes ?
     *
     * fichier_sous_activite reference livrable_sous_activite. Supprimer un
     * livrable qui porte des depots echouerait sur la cle etrangere, c'est a
     * dire une 500 illisible ; cette requete permet de la transformer en 409
     * explicite, comme pour une sous-activite.
     *
     * Les fichiers ne sont donc pas supprimes en cascade : un depot fait
     * partie de l'historique de l'activite, il ne disparait pas parce qu'une
     * ligne de brouillon a etee editee.
     */
    @Query(value = """
            SELECT COUNT(*) FROM fichier_sous_activite
            WHERE id_livrable_sous_activite = :id
            """,
            nativeQuery = true)
    long compterFichiers(@Param("id") Integer idLivrable);

    /**
     * Ce livrable appartient-il a CETTE sous-activite ?
     *
     * Le depot de fichiers porte trois identifiants : activite, sous-activite,
     * livrable. Les deux premiers sont deja valides par la lecture de la
     * sous-activite, qui applique le perimetre ; celui-ci ferme le troisieme.
     * Sans lui, un identifiant de livrable quelconque autoriserait d ecrire
     * dans un livrable d une autre sous-activite du meme perimetre, y compris
     * d une activite terminee.
     *
     * Une absence vaut refus et non introuvable : la distinction
     * interessait quand le service pouvait dire "ce livrable n existe pas" et
     * "il n est pas a vous". Ici les deux se confondent, et repondre par le
     * meme refus evite d reveler l existence d un livrable qu on ne voit pas.
     */
    Optional<Livrable> findByIdLivrableAndSousActiviteIdSousActivite(
            Integer idLivrable, Integer idSousActivite);

}
