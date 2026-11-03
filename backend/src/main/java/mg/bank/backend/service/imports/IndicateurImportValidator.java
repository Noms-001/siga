package mg.bank.backend.service.imports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteIndicateursGroupDTO;
import mg.bank.backend.dto.importactivite.AvertissementImportDTO;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportReferenceDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.repository.indicateur.IndicateurRepository;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class IndicateurImportValidator {

    private static final String VERSION_ATTENDUE = "1.0";
    private static final int LONGUEUR_MAX_DESIGNATION = 255;

    private final ImportActiviteLookup lookup;
    private final IndicateurRepository indicateurRepository;

    @Transactional(readOnly = true)
    public RapportValidationDTO valider(IndicateurImportRequest request) {
        List<ErreurImportDTO> erreurs = new ArrayList<>();
        List<AvertissementImportDTO> avertissements = new ArrayList<>();

        validerEntete(request, erreurs);

        int nbGroupes = 0;
        if (request != null && request.getActivites() != null) {
            nbGroupes = request.getActivites().size();
            Set<String> vues = new HashSet<>();
            for (int i = 0; i < request.getActivites().size(); i++) {
                validerGroupe(request.getActivites().get(i), i + 1,
                        vues, erreurs, avertissements);
            }
        }

        return buildRapport(erreurs, avertissements, nbGroupes);
    }

    private void validerEntete(IndicateurImportRequest r, List<ErreurImportDTO> erreurs) {
        if (r == null) {
            erreurs.add(erreur(null, null, "Le corps de la requête d'import est vide."));
            return;
        }
        if (estVide(r.getVersion())) {
            erreurs.add(erreur(null, "version", "La version est obligatoire."));
        } else if (!VERSION_ATTENDUE.equals(r.getVersion().trim())) {
            erreurs.add(erreur(null, "version",
                    "Version non supportée : « " + r.getVersion() + " ». Attendu : « "
                            + VERSION_ATTENDUE + " »."));
        }
        if (r.getActivites() == null || r.getActivites().isEmpty()) {
            erreurs.add(erreur(null, "activites",
                    "Le lot doit contenir au moins une activité."));
        }
    }

    private void validerGroupe(ActiviteIndicateursGroupDTO g, int index,
                               Set<String> vues,
                               List<ErreurImportDTO> erreurs,
                               List<AvertissementImportDTO> avertissements) {
        String id = "activité #" + index;
        if (g == null) {
            erreurs.add(erreur(id, null, "L'entrée est un objet null."));
            return;
        }
        if (estVide(g.getReference())) {
            erreurs.add(erreur(id, "reference",
                    "La référence de l'activité est obligatoire."));
            return;
        }
        id = g.getReference().trim();
        if (!vues.add(id.toLowerCase(Locale.ROOT))) {
            erreurs.add(erreur(id, "reference",
                    "L'activité « " + id + " » est ciblée plusieurs fois dans le lot."));
            return;
        }

        lookup.parReferenceAvecErreurs(id, id, erreurs);

        if (g.getIndicateurs() == null || g.getIndicateurs().isEmpty()) {
            erreurs.add(erreur(id, "indicateurs",
                    "Au moins un indicateur est requis."));
            return;
        }

        Set<String> désignations = new HashSet<>();
        for (int j = 0; j < g.getIndicateurs().size(); j++) {
            IndicateurImportReferenceDTO ind = g.getIndicateurs().get(j);
            String champ = "indicateurs[" + j + "]";
            if (ind == null) {
                erreurs.add(erreur(id, champ,
                        "L'indicateur #" + (j + 1) + " est un objet null."));
                continue;
            }
            if (estVide(ind.getDesignation())) {
                erreurs.add(erreur(id, champ + ".designation",
                        "La désignation de l'indicateur #" + (j + 1) + " est obligatoire."));
                continue;
            }
            if (ind.getDesignation().length() > LONGUEUR_MAX_DESIGNATION) {
                erreurs.add(erreur(id, champ + ".designation",
                        "La désignation de l'indicateur #" + (j + 1)
                                + " dépasse " + LONGUEUR_MAX_DESIGNATION + " caractères."));
            }
            String cle = ind.getDesignation().trim().toLowerCase(Locale.ROOT);
            if (!désignations.add(cle)) {
                erreurs.add(erreur(id, champ + ".designation",
                        "L'indicateur « " + ind.getDesignation().trim()
                                + " » apparaît plusieurs fois sur la même activité."));
                continue;
            }
            long n = indicateurRepository.countByDesignationIgnoreCase(ind.getDesignation().trim());
            if (n == 0) {
                avertissements.add(avertissement(id, champ + ".designation",
                        "L'indicateur « " + ind.getDesignation()
                                + " » sera créé automatiquement à l'import."));
            } else if (n > 1) {
                erreurs.add(erreur(id, champ + ".designation",
                        "Plusieurs indicateurs portent la désignation « "
                                + ind.getDesignation() + " » : impossible de choisir."));
            }
        }
    }

    private RapportValidationDTO buildRapport(List<ErreurImportDTO> erreurs,
                                              List<AvertissementImportDTO> avertissements,
                                              int nbGroupes) {
        boolean valide = erreurs.isEmpty();
        String message = valide
                ? (avertissements.isEmpty()
                        ? "Toutes les données du lot sont valides."
                        : "Les données sont valides, "
                                + avertissements.size() + " avertissement(s).")
                : "Des erreurs ont été détectées dans les données à importer.";
        return RapportValidationDTO.builder()
                .valide(valide).message(message)
                .nombreActivites(nbGroupes)
                .nombreErreurs(erreurs.size())
                .nombreAvertissements(avertissements.size())
                .erreurs(erreurs).avertissements(avertissements)
                .apercu(null)
                .build();
    }

    private ErreurImportDTO erreur(String a, String c, String m) {
        return ErreurImportDTO.builder().activite(a).champ(c).message(m).build();
    }

    private AvertissementImportDTO avertissement(String a, String c, String m) {
        return AvertissementImportDTO.builder().activite(a).champ(c).message(m).build();
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}