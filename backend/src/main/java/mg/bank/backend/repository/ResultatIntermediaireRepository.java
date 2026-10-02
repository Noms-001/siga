package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.ResultatIntermediaire;

public interface ResultatIntermediaireRepository
        extends JpaRepository<ResultatIntermediaire, Integer> {

    /**
     * Resultats intermediaires d une activite.
     *
     * Le tri par identifiant n est pas un gout : c'est ce qui permet a la
     * synchronisation de comparer les lignes renvoyees par le formulaire a
     * celles qui existent, ligne a ligne, dans l'ordre de saisie. Un tri par
     * designation les melangerait et aucune ligne ne serait reconnue.
     *
     * La methode derivee suffit : il n'y a rien a ecrire qui ne soit deja le
     * nom de la propriete et de l'ordre.
     */
    List<ResultatIntermediaire> findByActiviteIdActiviteOrderByIdResultatIntermediaireAsc(
            Integer idActivite);

}
