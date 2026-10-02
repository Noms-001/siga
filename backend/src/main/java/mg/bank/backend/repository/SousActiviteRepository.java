package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.SousActivite;

public interface SousActiviteRepository
        extends JpaRepository<SousActivite, Integer> {

    /**
     * Sous-activites d une activite, triees de facon stable pour que la
     * synchronisation d une modification sache quoi elle doit ajouter,
     * mettre a jour et supprimer.
     */
    List<SousActivite> findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
            Integer idActivite);

    /**
     * Un code de sous-activite est-il deja pris ?
     *
     * Interroge sur toute la table et non sur les seules lignes d une
     * activite, parce que UNIQUE(code) est global lui aussi : un code saisi
     * doit etre libre dans la base entiere, sous peine d une violation de
     * contrainte remontee en 500 alors qu elle releve de la saisie.
     *
     * IgnoreCase, pour la meme raison que sur activite : l index UNIQUE
     * reste sensible a la casse, "sa-1-1" et "SA-1-1" se recopient et se
     * retapent sans que le lecteur y voie deux codes.
     *
     * Requete derivee, sans @Query : il n y a rien a ecrire qui n soit
     * deja le nom de la methode.
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Une sous-activite est-elle encore attachee a autre chose ?
     *
     * Trois tables pointent vers sous_activite :
     * livrable_sous_activite, affectation_sous_activite et
     * avancement_sous_activite. Supprimer une ligne qui en depend produit
     * une violation de cle etrangere, c est-a-dire une 500 illisible pour
     * l utilisateur ; cette requete permet de la transformer en 409
     * explicite.
     *
     * On interroge chaque table separement et on additionne : une seule
     * UNION de comptages sur des noms de tables distincts n apporte rien,
     * et un LEFT JOIN sur les trois tables compterait un produit
     * cartesian, pas une somme de lignes existantes.
     */
    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM livrable_sous_activite
                WHERE id_sous_activite = :id)
            + (SELECT COUNT(*) FROM affectation_sous_activite
                WHERE id_sous_activite = :id)
            + (SELECT COUNT(*) FROM avancement_sous_activite
                WHERE id_sous_activite = :id)
            """,
            nativeQuery = true)
    long compterDependances(@Param("id") Integer idSousActivite);

}
