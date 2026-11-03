package mg.bank.backend.service.imports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteSousActivitesGroupDTO;
import mg.bank.backend.dto.importactivite.AvertissementImportDTO;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.dto.importactivite.LivrableImportDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportRequest;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.repository.activite.SousActiviteRepository;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SousActiviteImportValidator {

    private static final String VERSION_ATTENDUE = "1.0";
    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 2100;
    private static final int LONGUEUR_MAX_SOUS = 255;
    private static final int LONGUEUR_MAX_LIVRABLE = 250;

    private final ImportActiviteLookup lookup;
    private final SousActiviteRepository sousActiviteRepository;

    @Transactional(readOnly = true)
    public RapportValidationDTO valider(SousActiviteImportRequest request) {
        List<ErreurImportDTO> erreurs = new ArrayList<>();
        List<AvertissementImportDTO> avertissements = new ArrayList<>();

        validerEntete(request, erreurs);

        int nbGroupes = 0;
        if (request != null && request.getActivites() != null) {
            nbGroupes = request.getActivites().size();
            Set<String> vues = new HashSet<>();
            Set<String> codesVus = new HashSet<>();
            for (int i = 0; i < request.getActivites().size(); i++) {
                validerGroupe(request.getActivites().get(i), i + 1, vues, codesVus, erreurs, avertissements);
            }
        }

        return buildRapport(erreurs, avertissements, nbGroupes);
    }

    private void validerEntete(SousActiviteImportRequest r, List<ErreurImportDTO> erreurs) {
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
        if (r.getAnnee() == null) {
            erreurs.add(erreur(null, "annee", "L'année de l'import est obligatoire."));
        } else if (r.getAnnee() < ANNEE_MIN || r.getAnnee() > ANNEE_MAX) {
            erreurs.add(erreur(null, "annee",
                    "L'année doit être comprise entre " + ANNEE_MIN + " et " + ANNEE_MAX + "."));
        }
        if (r.getActivites() == null || r.getActivites().isEmpty()) {
            erreurs.add(erreur(null, "activites",
                    "Le lot doit contenir au moins une activité."));
        }
    }

    private void validerGroupe(ActiviteSousActivitesGroupDTO g, int index,
                               Set<String> vues,
                               Set<String> codesVus,
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

        Activite parent = lookup.parReferenceAvecErreurs(id, id, erreurs);

        if (g.getSousActivites() == null || g.getSousActivites().isEmpty()) {
            erreurs.add(erreur(id, "sousActivites",
                    "Au moins une sous-activité est requise."));
            return;
        }

        Set<String> désignations = new HashSet<>();
        for (int j = 0; j < g.getSousActivites().size(); j++) {
                validerSousActivite(id, g.getSousActivites().get(j), j, parent,
                    désignations, codesVus, erreurs, avertissements);
        }
    }

    private void validerSousActivite(String refActivite, SousActiviteImportDTO sa, int index,
                                     Activite parent,
                                     Set<String> désignations,
                                     Set<String> codesVus,
                                     List<ErreurImportDTO> erreurs,
                                     List<AvertissementImportDTO> avertissements) {
        String champ = "sousActivites[" + index + "]";
        String libelle = "sous-activité #" + (index + 1);

        if (sa == null) {
            erreurs.add(erreur(refActivite, champ, "La " + libelle + " est un objet null."));
            return;
        }

        if (!estVide(sa.getCode())) {
            String code = sa.getCode().trim();
            if (code.length() > 20) {
                erreurs.add(erreur(refActivite, champ + ".code",
                        "Le code de la sous-activité ne doit pas dépasser 20 caractères."));
                } else if (parent != null && (parent.getCode() == null || parent.getCode().length() < 3
                    || !parent.getCode().startsWith("A-") || !code.matches("^SA-"
                        + java.util.regex.Pattern.quote(parent.getCode().substring(2))
                        + "-\\d{2,}$"))) {
                erreurs.add(erreur(refActivite, champ + ".code",
                        "Le code doit reprendre exactement les références de l'activité parente."));
                } else if ((parent != null && sousActiviteRepository
                    .existsByCodeIgnoreCaseAndActivite_IdActivite(code, parent.getIdActivite()))
                    || !codesVus.add((parent == null ? "?" : parent.getIdActivite())
                        + ":" + code.toLowerCase(Locale.ROOT))) {
                erreurs.add(erreur(refActivite, champ + ".code",
                        "Ce code de sous-activité est déjà utilisé."));
            }
        }

        if (estVide(sa.getDesignation())) {
            erreurs.add(erreur(refActivite, champ + ".designation",
                    "La désignation de la " + libelle + " est obligatoire."));
        } else {
            if (sa.getDesignation().length() > LONGUEUR_MAX_SOUS) {
                erreurs.add(erreur(refActivite, champ + ".designation",
                        "La désignation de la " + libelle + " dépasse "
                                + LONGUEUR_MAX_SOUS + " caractères."));
            }
            String cle = sa.getDesignation().trim().toLowerCase(Locale.ROOT);
            if (!désignations.add(cle)) {
                avertissements.add(avertissement(refActivite, champ + ".designation",
                        "La désignation « " + sa.getDesignation().trim()
                                + " » apparaît plusieurs fois sur la même activité."));
            }
        }

        if (sa.getDateDebut() == null) {
            erreurs.add(erreur(refActivite, champ + ".dateDebut",
                    "La date de début de la " + libelle + " est obligatoire."));
        }
        if (sa.getDateFin() == null) {
            erreurs.add(erreur(refActivite, champ + ".dateFin",
                    "La date de fin de la " + libelle + " est obligatoire."));
        }
        if (sa.getDateDebut() != null && sa.getDateFin() != null
                && sa.getDateFin().isBefore(sa.getDateDebut())) {
            erreurs.add(erreur(refActivite, champ + ".dateFin",
                    "La date de fin doit être ≥ à la date de début."));
        }

        if (sa.getLivrables() != null) {
            for (int k = 0; k < sa.getLivrables().size(); k++) {
                LivrableImportDTO l = sa.getLivrables().get(k);
                String champL = champ + ".livrables[" + k + "]";
                if (l == null) {
                    erreurs.add(erreur(refActivite, champL,
                            "Le livrable #" + (k + 1) + " est un objet null."));
                } else if (estVide(l.getDesignation())) {
                    erreurs.add(erreur(refActivite, champL + ".designation",
                            "La désignation du livrable est obligatoire."));
                } else if (l.getDesignation().length() > LONGUEUR_MAX_LIVRABLE) {
                    erreurs.add(erreur(refActivite, champL + ".designation",
                            "La désignation du livrable dépasse "
                                    + LONGUEUR_MAX_LIVRABLE + " caractères."));
                }
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
                .valide(valide)
                .message(message)
                .nombreActivites(nbGroupes)
                .nombreErreurs(erreurs.size())
                .nombreAvertissements(avertissements.size())
                .erreurs(erreurs)
                .avertissements(avertissements)
                .apercu(null)
                .build();
    }

    private ErreurImportDTO erreur(String activite, String champ, String message) {
        return ErreurImportDTO.builder()
                .activite(activite).champ(champ).message(message).build();
    }

    private AvertissementImportDTO avertissement(String a, String c, String m) {
        return AvertissementImportDTO.builder()
                .activite(a).champ(c).message(m).build();
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}