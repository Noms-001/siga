package mg.bank.backend.dto.importactivite;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une activite a importer.
 *
 * <p>Les referentiels simples (service, priorite, type d'activite, site) sont
 * transportes par leur libelle, conformement au format d'echange. Les
 * referentiels composes (objectif specifique, procedure) sont des objets
 * minimaux (code, designation). Aucun identifiant PostgreSQL n'est present
 * dans ce DTO : la resolution en entite est de la responsabilite du service
 * d'import (etape 3).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteImportDTO {

    /** Facultatif : genere par le backend s'il est absent (etape 2). */
    private String code;

    /** Facultatif en base ; obligatoire pour une PTA (regle metier a l'etape 2). */
    private String reference;

    private String designation;

    /** Correspond a la colonne {@code activite.date_debut_prevue}. */
    private LocalDate dateDebut;

    /** Correspond a la colonne {@code activite.date_fin_prevue}. */
    private LocalDate dateFin;

    /** Correspond a la colonne {@code activite.date_debut_reelle}. */
    private LocalDate dateDebutReelle;

    /** Correspond a la colonne {@code activite.date_fin_reelle}. */
    private LocalDate dateFinReelle;

    /** Code de la priorite. Obligatoire en base. */
    private String priorite;

    /** Designation du type d'activite. Facultatif. */
    private String typeActivite;

    /** Nom du site. Facultatif. */
    private String site;

    /**
     * Nullable = activite NON PTA (regle identique a celle portee par
     * l'entite {@code Activite}).
     */
    private ObjectifSpecifiqueImportReferenceDTO objectifSpecifique;

    private List<SousActiviteImportDTO> sousActivites;

    private List<ResultatIntermediaireImportDTO> resultatsIntermediaires;

    /** References par designation, sans id. */
    private List<IndicateurImportReferenceDTO> indicateurs;

    /** References par designation, sans id. */
    private List<OrigineImportReferenceDTO> origines;
}