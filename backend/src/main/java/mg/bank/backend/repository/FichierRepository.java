package mg.bank.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Fichier;

/**
 * Depots de fichier sur livrable.
 *
 * Uniquement pour la lecture par identifiant, que JpaRepository fournit deja,
 * et pour l ecriture du depot. La liste des fichiers d une sous-activite n
 * passe PAS par ici : elle est lue en une requete native
 * (ActiviteRepository.findFichiersSousActivite) qui regroupe par livrable, et
 * aucune requete ici ne ferait cela aussi simplement.
 */
public interface FichierRepository extends JpaRepository<Fichier, Integer> {
}
