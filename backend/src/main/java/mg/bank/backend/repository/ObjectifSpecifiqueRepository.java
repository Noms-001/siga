package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.ObjectifSpecifique;
import mg.bank.backend.repository.projection.ObjectifAutocompleteRow;

public interface ObjectifSpecifiqueRepository
        extends JpaRepository<ObjectifSpecifique, Integer> {

    /**
     * Un code d'objectif est-il deja pris ?
     *
     * La colonne est UNIQUE, mais on ne compte pas dessus : une violation
     * d'unicite remonte en 500, et dire a l'utilisateur que son code est deja
     * pris est une erreur de saisie, pas une panne. La comparaison ignore la
     * casse pour que `bct/1` et `BCT/1` ne puissent pas se succeder.
     *
     * La course reste possible entre cette verification et l'insertion : deux
     * creations simultanees du meme code passent toutes deux ici. C'est la
     * contrainte unique qui tranche, et c'est elle qui a le dernier mot.
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Autocomplete objectif : code, designation ou annee.
     *
     * L annee est un axe de recherche a part entiere, et pas un detail :
     * les objectifs sont organises par annee, et un utilisateur qui cherche
     * l objectif de l annee en cours tape "2026", pas un code. Comme la
     * colonne est un entier et le motif un texte, la comparaison passe par
     * un CAST explicite plutot que par un postfixe, le code des objectifs
     * contenant deja l annee.
     *
     * L annee est rendue dans libelleSecondaire : deux objectifs de
     * memes code court et de memes designation ne se distinguent que par elle.
     *
     * prochainNumero accompagne la suggestion parce que le code et la
     * reference d une activite PTA sont deduits de l'objectif (A-<annee>-<n>
     * et <code>_n) : sans lui, le formulaire ne pourrait proposer que le
     * numero 1, deja pris sur tout objectif qui a deja une activite.
     *
     * Le compte est une SOUS-requete correlee et non un LEFT JOIN suivi d un
     * COUNT : un JOIN sur activite multiplierait chaque objectif par son
     * nombre d'activites, et le comptage renverrait alors ce produit, pas le
     * nombre de lignes reelles. Le cast en int est explicite car COUNT
     * renvoie un bigint que la projection annonce en Integer.
     */
    @Query(value = """
            SELECT os.id_objectif_specifique                AS id,
                   os.code                                  AS code,
                   os.designation                           AS libelle,
                   os.annee                                 AS annee,
                   ((SELECT COUNT(*) + 1
                       FROM activite a
                      WHERE a.id_objectif_specifique = os.id_objectif_specifique))::int
                                                          AS prochainNumero
            FROM objectif_specifique os
            WHERE (:recherche IS NULL
                   OR os.code ILIKE :motif
                   OR os.designation ILIKE :motif
                   OR CAST(os.annee AS TEXT) ILIKE :motif)
            ORDER BY os.annee DESC, os.code
            LIMIT :limite
            """,
            nativeQuery = true)
    List<ObjectifAutocompleteRow> autocomplete(
            String recherche,
            String motif,
            int limite);

}
