package mg.bank.backend.repository.referentiel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.repository.projection.referentiel.ObjectifAutocompleteRow;

public interface ObjectifSpecifiqueRepository
              extends JpaRepository<ObjectifSpecifique, Integer> {

       boolean existsByCodeIgnoreCase(String code);

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
                     """, nativeQuery = true)
       List<ObjectifAutocompleteRow> autocomplete(
                     String recherche,
                     String motif,
                     int limite);

       Optional<ObjectifSpecifique> findByCodeIgnoreCase(String code);

       boolean existsByCodeIgnoreCaseAndAnnee(String code, Integer annee);

       Optional<ObjectifSpecifique> findByCodeIgnoreCaseAndAnnee(String code, Integer annee);

       /**
        * Toutes les occurrences d'un code (toutes années confondues), pour
        * l'autocomplete.
        */
       List<ObjectifSpecifique> findByCodeIgnoreCaseOrderByAnneeDesc(String code);

}
