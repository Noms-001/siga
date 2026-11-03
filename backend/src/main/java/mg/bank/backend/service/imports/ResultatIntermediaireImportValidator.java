package mg.bank.backend.service.imports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteResultatsGroupDTO;
import mg.bank.backend.dto.importactivite.AvertissementImportDTO;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportRequest;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ResultatIntermediaireImportValidator {

    private static final String VERSION_ATTENDUE = "1.0";
    private static final int LONGUEUR_MAX = 255;

    private final ImportActiviteLookup lookup;

    @Transactional(readOnly = true)
    public RapportValidationDTO valider(ResultatIntermediaireImportRequest request) {
        List<ErreurImportDTO> erreurs = new ArrayList<>();
        List<AvertissementImportDTO> avertissements = new ArrayList<>();

        validerEntete(request, erreurs);

        int nbGroupes = 0;
        if (request != null && request.getActivites() != null) {
            nbGroupes = request.getActivites().size();
            Set<String> vues = new HashSet<>();
            for (int i = 0; i < request.getActivites().size(); i++) {
                validerGroupe(request.getActivites().get(i), i + 1, vues, erreurs);
            }
        }

        return buildRapport(erreurs, avertissements, nbGroupes);
    }

    private void validerEntete(ResultatIntermediaireImportRequest r, List<ErreurImportDTO> erreurs) {
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

    private void validerGroupe(ActiviteResultatsGroupDTO g, int index,
                               Set<String> vues, List<ErreurImportDTO> erreurs) {
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

        if (g.getResultatsIntermediaires() == null || g.getResultatsIntermediaires().isEmpty()) {
            erreurs.add(erreur(id, "resultatsIntermediaires",
                    "Au moins un résultat intermédiaire est requis."));
            return;
        }

        Set<String> désignations = new HashSet<>();
        for (int j = 0; j < g.getResultatsIntermediaires().size(); j++) {
            ResultatIntermediaireImportDTO r = g.getResultatsIntermediaires().get(j);
            String champ = "resultatsIntermediaires[" + j + "]";
            if (r == null) {
                erreurs.add(erreur(id, champ,
                        "Le résultat #" + (j + 1) + " est un objet null."));
                continue;
            }
            if (estVide(r.getDesignation())) {
                erreurs.add(erreur(id, champ + ".designation",
                        "La désignation du résultat #" + (j + 1) + " est obligatoire."));
                continue;
            }
            if (r.getDesignation().length() > LONGUEUR_MAX) {
                erreurs.add(erreur(id, champ + ".designation",
                        "La désignation du résultat #" + (j + 1)
                                + " dépasse " + LONGUEUR_MAX + " caractères."));
            }
            String cle = r.getDesignation().trim().toLowerCase(Locale.ROOT);
            if (!désignations.add(cle)) {
                erreurs.add(erreur(id, champ + ".designation",
                        "Le résultat « " + r.getDesignation().trim()
                                + " » apparaît plusieurs fois sur la même activité."));
            }
        }
    }

    private RapportValidationDTO buildRapport(List<ErreurImportDTO> erreurs,
                                              List<AvertissementImportDTO> avertissements,
                                              int nbGroupes) {
        boolean valide = erreurs.isEmpty();
        String message = valide
                ? "Toutes les données du lot sont valides."
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

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}