package mg.bank.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;

import mg.bank.backend.dto.ActiviteFiltreCriteria;
import mg.bank.backend.dto.OptionDTO;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.repository.projection.OptionRow;

public final class ActiviteQuerySupport {

    public static final Sort TRI_PAR_DEFAUT =
            Sort.by(Sort.Direction.DESC, "dateCreationTri");

    private ActiviteQuerySupport() {
    }

    public record Arguments(
            Boolean pta,
            String recherche,
            Integer objectifId,
            String objectifRecherche,
            Integer servicePerimetre,
            Integer departementPerimetre,
            Integer serviceDemande,
            Integer prioriteId,
            Integer typeActiviteId,
            Integer siteId,
            String statutCode,
            String codesTerminaux,
            Integer annee,
            Integer moisDebut,
            Integer moisFin,
            LocalDate dateDebut,
            LocalDate dateFin) {
    }

    public static Arguments arguments(ActiviteFiltreCriteria filtres, PerimetreUtilisateur perimetre) {
        Boolean pta = filtres == null || filtres.getPta() == null
                ? Boolean.TRUE
                : filtres.getPta();

        boolean horsPta = Boolean.FALSE.equals(pta);

        Integer servicePerimetre = perimetre.estRattacheService()
                ? perimetre.getIdService()
                : null;

        Integer departementPerimetre = perimetre.estNiveauDepartement()
                ? perimetre.getIdDepartement()
                : null;

        Integer serviceDemande = filtres == null ? null : filtres.getServiceId();

        return new Arguments(
                pta,
                motif(filtres == null ? null : filtres.getSearch()),
                horsPta ? null : (filtres == null ? null : filtres.getObjectifSpecifiqueId()),
                horsPta ? null : motif(filtres == null ? null : filtres.getObjectifSearch()),
                servicePerimetre,
                departementPerimetre,
                serviceDemande,
                filtres == null ? null : filtres.getPrioriteId(),
                filtres == null ? null : filtres.getTypeActiviteId(),
                filtres == null ? null : filtres.getSiteId(),
                videSiBlanc(filtres == null ? null : filtres.getStatutCode()),
                codesTerminaux(),
                filtres == null ? null : filtres.getAnnee(),
                moisDebut(filtres == null ? null : filtres.getTrimestre()),
                moisFin(filtres == null ? null : filtres.getTrimestre()),
                filtres == null ? null : filtres.getDateDebut(),
                filtres == null ? null : filtres.getDateFin());
    }

    public static Integer moisDebut(Integer trimestre) {
        if (trimestre == null || trimestre < 1 || trimestre > 4) {
            return null;
        }
        return (trimestre - 1) * 3 + 1;
    }

    public static Integer moisFin(Integer trimestre) {
        if (trimestre == null || trimestre < 1 || trimestre > 4) {
            return null;
        }
        return (trimestre - 1) * 3 + 3;
    }

    public static String codesTerminaux() {
        return String.join(",", StatutActiviteEnum.codesTerminaux());
    }

    public static String motif(String recherche) {
        String valeur = videSiBlanc(recherche);
        return valeur == null ? null : "%" + valeur + "%";
    }

    public static String videSiBlanc(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    public static Sort assainir(Sort sort) {
        List<String> autorisees = List.of(
                "dateCreationTri", "code", "reference", "designation",
                "dateDebutPrevue", "dateFinPrevue", "avancement", "statutCode");

        List<Sort.Order> retenus = sort.stream()
                .filter(ordre -> autorisees.contains(ordre.getProperty()))
                .toList();

        return retenus.isEmpty() ? TRI_PAR_DEFAUT : Sort.by(retenus);
    }

    public static List<OptionDTO> versOptions(List<OptionRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(r -> OptionDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .build())
                .toList();
    }

    public static long nulSiAbsent(Long valeur) {
        return valeur == null ? 0L : valeur;
    }
}
