package mg.bank.backend.service.imports;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.importactivite.ActiviteImportDTO;
import mg.bank.backend.dto.importactivite.ActiviteImportRequest;
import mg.bank.backend.dto.importactivite.ApercuActiviteDTO;
import mg.bank.backend.dto.importactivite.AvertissementImportDTO;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportReferenceDTO;
import mg.bank.backend.dto.importactivite.LivrableImportDTO;
import mg.bank.backend.dto.importactivite.ObjectifSpecifiqueImportReferenceDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportDTO;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.model.referentiel.Service;
import mg.bank.backend.model.validation.Procedure;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;
import mg.bank.backend.repository.referentiel.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.referentiel.PrioriteRepository;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.referentiel.SiteRepository;
import mg.bank.backend.repository.referentiel.TypeActiviteRepository;
import mg.bank.backend.repository.validation.ProcedureRepository;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.common.PorteeService;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ActiviteImportValidator {

    private static final String VERSION_ATTENDUE = "1.0";

    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 2100;

    private static final int LONGUEUR_MAX_DESIGNATION_ACTIVITE = 255;
    private static final int LONGUEUR_MAX_DESIGNATION_SOUS = 255;
    private static final int LONGUEUR_MAX_DESIGNATION_LIVRABLE = 250;
    private static final int LONGUEUR_MAX_DESIGNATION_RESULTAT = 255;

    private final ActiviteService activiteService;
    private final PorteeService porteeService;
    private final ActiviteRepository activiteRepository;

    private final ServiceRepository serviceRepository;
    private final ProcedureRepository procedureRepository;
    private final PrioriteRepository prioriteRepository;
    private final TypeActiviteRepository typeActiviteRepository;
    private final SiteRepository siteRepository;
    private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
    private final IndicateurRepository indicateurRepository;

    @Transactional(readOnly = true)
    public RapportValidationDTO valider(ActiviteImportRequest request) {
        List<ErreurImportDTO> erreurs = new ArrayList<>();
        List<AvertissementImportDTO> avertissements = new ArrayList<>();

        validerEntete(request, erreurs);

        Service serviceRacine = null;
        Procedure procedureRacine = null;
        if (request != null) {
            serviceRacine = resoudreServiceRacine(request, erreurs);
            procedureRacine = resoudreProcedureRacine(request, erreurs);
        }

        int nombreActivites = 0;
        if (request != null && request.getActivites() != null) {
            nombreActivites = request.getActivites().size();

            Map<String, Integer> designationsVues = new HashMap<>();
            Set<String> codesVus = new HashSet<>();

            for (int i = 0; i < request.getActivites().size(); i++) {
                validerActivite(
                        request.getActivites().get(i),
                        i + 1,
                        request.getTypeImport(),
                        request.getAnnee(),
                        request.getService(),
                        designationsVues,
                        codesVus,
                        erreurs,
                        avertissements);
            }
        }

        List<ApercuActiviteDTO> apercu = null;
        if (erreurs.isEmpty()
                && serviceRacine != null
                && request != null
                && request.getActivites() != null
                && !request.getActivites().isEmpty()
                && (Boolean.TRUE.equals(request.getActivitesDejaValidees())
                        || procedureRacine != null)) {
            apercu = construireApercu(request);
        }

        return buildRapport(erreurs, avertissements, nombreActivites, apercu);
    }

    /* ------------------------------------------------------------------ */
    /* Entete                                                              */
    /* ------------------------------------------------------------------ */

    private void validerEntete(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (request == null) {
            erreurs.add(erreur(null, null, "Le corps de la requête d'import est vide."));
            return;
        }

        if (estVide(request.getVersion())) {
            erreurs.add(erreur(null, "version",
                    "La version du format d'import est obligatoire."));
        } else if (!VERSION_ATTENDUE.equals(request.getVersion().trim())) {
            erreurs.add(erreur(null, "version",
                    "Version d'import non supportée : « " + request.getVersion()
                            + " ». Version attendue : « " + VERSION_ATTENDUE + " »."));
        }

        if (request.getService() == null) {
            erreurs.add(erreur(null, "service",
                    "Le service est obligatoire pour l'import."));
        }

        if (request.getAnnee() == null) {
            erreurs.add(erreur(null, "annee",
                    "L'année de l'import est obligatoire."));
        } else if (request.getAnnee() < ANNEE_MIN || request.getAnnee() > ANNEE_MAX) {
            erreurs.add(erreur(null, "annee",
                    "L'année doit être comprise entre " + ANNEE_MIN
                            + " et " + ANNEE_MAX + "."));
        }

        if (Boolean.FALSE.equals(request.getActivitesDejaValidees())
                && request.getProcedure() == null) {
            erreurs.add(erreur(null, "procedure",
                    "La procédure est obligatoire pour des activités non encore validées."));
        }

        if (request.getTypeImport() == null) {
            erreurs.add(erreur(null, "typeImport",
                    "Le type d'import est obligatoire (PTA ou NON_PTA)."));
        }

        if (request.getActivitesDejaValidees() == null) {
            erreurs.add(erreur(null, "activitesDejaValidees",
                    "Le paramètre « activitesDejaValidees » est obligatoire."));
        }

        if (request.getActivites() == null) {
            erreurs.add(erreur(null, "activites",
                    "Le tableau « activites » est obligatoire."));
        } else if (request.getActivites().isEmpty()) {
            erreurs.add(erreur(null, "activites",
                    "Le lot d'import doit contenir au moins une activité."));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Service et procedure racine                                         */
    /* ------------------------------------------------------------------ */

    private Service resoudreServiceRacine(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (request.getService() == null) {
            return null;
        }

        Service service = serviceRepository.findById(request.getService()).orElse(null);
        if (service == null) {
            erreurs.add(erreur(null, "service",
                    "Le service indiqué (id=" + request.getService() + ") est introuvable."));
            return null;
        }

        if (!Boolean.TRUE.equals(service.getActif())) {
            erreurs.add(erreur(null, "service",
                    "Le service indiqué (id=" + request.getService() + ") n'est pas actif."));
            return null;
        }

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        if (!autorise(service, perimetre)) {
            erreurs.add(erreur(null, "service",
                    "Vous n'êtes pas autorisé à importer des activités pour ce service."));
            return null;
        }
        return service;
    }

    private Procedure resoudreProcedureRacine(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (Boolean.TRUE.equals(request.getActivitesDejaValidees())) {
            return null;
        }
        if (request.getProcedure() == null) {
            return null;
        }

        Procedure procedure = procedureRepository.findById(request.getProcedure()).orElse(null);
        if (procedure == null) {
            erreurs.add(erreur(null, "procedure",
                    "La procédure indiquée (id=" + request.getProcedure() + ") est introuvable."));
            return null;
        }
        return procedure;
    }

    private boolean autorise(Service service, PerimetreUtilisateur perimetre) {
        if (perimetre == null) return false;
        if (!porteeService.serviceAccessible(service)) return false;
        if (perimetre.estRattacheService()) {
            return service.getIdService() != null
                    && service.getIdService().equals(perimetre.getIdService());
        }
        if (perimetre.estNiveauDepartement()) {
            return service.getDepartement() != null
                    && service.getDepartement().getIdDepartement() != null
                    && service.getDepartement().getIdDepartement()
                            .equals(perimetre.getIdDepartement());
        }
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Activite                                                            */
    /* ------------------------------------------------------------------ */

    private void validerActivite(
            ActiviteImportDTO a,
            int index,
            TypeImport typeImport,
            Integer annee,
            Integer idService,
            Map<String, Integer> designationsVues,
            Set<String> codesVus,
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements) {

        if (a == null) {
            erreurs.add(erreur(null, null,
                    "Activité #" + index + " : objet null."));
            return;
        }

        String id = identifiantActivite(a, index);

        // --- Designation ---
        if (estVide(a.getDesignation())) {
            erreurs.add(erreur(id, "designation",
                    "La désignation de l'activité est obligatoire."));
        } else if (a.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_ACTIVITE) {
            erreurs.add(erreur(id, "designation",
                    "La désignation de l'activité dépasse "
                            + LONGUEUR_MAX_DESIGNATION_ACTIVITE + " caractères."));
        }

        if (!estVide(a.getDesignation())) {
            String cleDesignation = a.getDesignation().trim().toLowerCase(Locale.ROOT);
            Integer premiereOccurrence = designationsVues.putIfAbsent(cleDesignation, index);
            if (premiereOccurrence != null) {
                avertissements.add(avertissement(id, "designation",
                        "La désignation « " + a.getDesignation().trim()
                                + " » est identique à celle de l'activité #"
                                + premiereOccurrence
                                + ". Vérifiez qu'il ne s'agit pas d'un doublon."));
            }
        }

        // --- Objectif specifique ---
        boolean estPta = TypeImport.PTA == typeImport;
        if (estPta) {
            verifierObjectifSpecifiquePta(
                    id,
                    annee,
                    a.getObjectifSpecifique(),
                    erreurs,
                    avertissements);
        } else if (TypeImport.NON_PTA == typeImport) {
            if (a.getObjectifSpecifique() != null) {
                erreurs.add(erreur(id, "objectifSpecifique",
                        "L'objectif spécifique doit être absent pour un import NON_PTA."));
            }
        }

        validerCodeFourni(a, typeImport, annee, idService, id, codesVus, erreurs);

        // --- Priorite (facultative : NORMALE par defaut si absente) ---
        if (!estVide(a.getPriorite())
                && !prioriteRepository.existsByCodeIgnoreCase(a.getPriorite().trim())) {
            erreurs.add(erreur(id, "priorite",
                    "La priorité « " + a.getPriorite() + " » est introuvable."));
        }

        // --- Type d'activite (facultatif; cree automatiquement si absent) ---
        if (!estVide(a.getTypeActivite())) {
            long n = typeActiviteRepository.countByDesignationIgnoreCase(a.getTypeActivite().trim());
            if (n == 0) {
                avertissements.add(avertissement(id, "typeActivite",
                        "Le type d'activité « " + a.getTypeActivite()
                                + " » n'existe pas encore : il sera créé automatiquement à l'import."));
            } else if (n > 1) {
                erreurs.add(erreur(id, "typeActivite",
                        "Plusieurs types d'activité portent la désignation « "
                                + a.getTypeActivite() + " » : impossible de choisir."));
            }
        }

        // --- Site (facultatif) ---
        if (!estVide(a.getSite())) {
            long n = siteRepository.countByNomIgnoreCase(a.getSite().trim());
            if (n == 0) {
                erreurs.add(erreur(id, "site",
                        "Le site « " + a.getSite() + " » est introuvable."));
            } else if (n > 1) {
                erreurs.add(erreur(id, "site",
                        "Plusieurs sites portent le nom « " + a.getSite()
                                + " » : impossible de choisir."));
            }
        }

        // --- Sous-activites (contenu) ---
        validerSousActivites(id, a.getSousActivites(), erreurs);

        // --- Resultats intermediaires ---
        validerResultatsIntermediaires(id, a.getResultatsIntermediaires(), erreurs);

        // --- Indicateurs ---
        validerIndicateurs(id, a.getIndicateurs(), erreurs, avertissements);
    }

    /**
     * Identifiant lisible d'une activite dans le rapport d'erreurs.
     *
     * Le code et la reference n'existant plus dans le DTO, l'identifiant
     * est la designation saisie, ou "activité #N" si la designation est
     * elle-meme vide.
     */
    private String identifiantActivite(ActiviteImportDTO a, int index) {
        if (a == null || estVide(a.getDesignation())) {
            return "activité #" + index;
        }
        return a.getDesignation().trim();
    }

    private void validerCodeFourni(
            ActiviteImportDTO activite,
            TypeImport typeImport,
            Integer annee,
            Integer idService,
            String idActivite,
            Set<String> codesVus,
            List<ErreurImportDTO> erreurs) {
        if (estVide(activite.getCode())) {
            return;
        }
        String code = activite.getCode().trim();
        String cleCode = code.toLowerCase(Locale.ROOT);
        boolean dejaUtilise = idService != null
            && activiteRepository.existsByCodeIgnoreCaseAndService_IdService(code, idService);
        if (dejaUtilise || !codesVus.add(cleCode)) {
            erreurs.add(erreur(idActivite, "code", "Ce code d'activité est déjà utilisé."));
            return;
        }
        if (code.length() > 20) {
            erreurs.add(erreur(idActivite, "code", "Le code ne doit pas dépasser 20 caractères."));
            return;
        }
        if (typeImport == TypeImport.NON_PTA) {
            if (!code.matches("^A-" + annee + "-\\d+$")) {
                erreurs.add(erreur(idActivite, "code",
                        "Le code NON_PTA doit respecter le format A-année-numéro, sans objectif spécifique."));
            }
            return;
        }
        if (typeImport != TypeImport.PTA || activite.getObjectifSpecifique() == null
                || estVide(activite.getObjectifSpecifique().getCode())) {
            return;
        }
        String codeObjectif = activite.getObjectifSpecifique().getCode().trim();
        java.util.regex.Matcher numeroObjectif = java.util.regex.Pattern
                .compile("(\\d+)(?!.*\\d)").matcher(codeObjectif);
        if (!numeroObjectif.find()) {
            return;
        }
        int numero;
        try {
            numero = Integer.parseInt(numeroObjectif.group(1));
        } catch (NumberFormatException exception) {
            erreurs.add(erreur(idActivite, "code",
                    "Le numéro de l'objectif spécifique est trop grand pour être codifié."));
            return;
        }
        String segmentObjectif = String.format(Locale.ROOT, "%02d", numero);
        if (!code.matches("^A-" + annee + "-" + segmentObjectif + "-\\d{2,}$")) {
            erreurs.add(erreur(idActivite, "code",
                    "Le code PTA doit respecter le format A-année-numéroObjectif-séquence et correspondre à l'objectif."));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Objectif specifique                                                 */
    /* ------------------------------------------------------------------ */

    private void verifierObjectifSpecifiquePta(
            String idActivite,
            Integer annee,
            ObjectifSpecifiqueImportReferenceDTO os,
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements) {

        if (os == null || estVide(os.getCode())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique",
                    "L'objectif spécifique est obligatoire pour un import PTA."));
            return;
        }

        if (estVide(os.getDesignation())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique.designation",
                    "La désignation de l'objectif spécifique est obligatoire pour un "
                            + "import PTA (elle est nécessaire à sa création si "
                            + "l'objectif n'existe pas encore)."));
        }

        ObjectifSpecifique existant = objectifSpecifiqueRepository
                .findByCodeIgnoreCase(os.getCode().trim())
                .orElse(null);

        if (existant == null) {
            if (!estVide(os.getDesignation())) {
                avertissements.add(avertissement(idActivite,
                        "objectifSpecifique",
                        "L'objectif spécifique « " + os.getCode()
                                + " » n'existe pas encore : il sera créé à l'import "
                                + "avec la désignation fournie et l'année "
                                + annee + "."));
            }
            return;
        }

        if (!estVide(os.getDesignation())
                && !os.getDesignation().trim().equalsIgnoreCase(existant.getDesignation())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique.designation",
                    "La désignation fournie pour l'objectif « " + os.getCode()
                            + " » ne correspond pas à celle de la base (attendu : « "
                            + existant.getDesignation() + " »)."));
        }

        if (annee != null && !annee.equals(existant.getAnnee())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique",
                    "L'objectif spécifique « " + os.getCode()
                            + " » existe déjà pour l'année " + existant.getAnnee()
                            + " ; l'import est configuré pour l'année " + annee + "."));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Sous-activites et livrables                                         */
    /* ------------------------------------------------------------------ */

    private void validerSousActivites(
            String idActivite,
            List<SousActiviteImportDTO> sousActivites,
            List<ErreurImportDTO> erreurs) {

        if (sousActivites == null) {
            return;
        }

        for (int j = 0; j < sousActivites.size(); j++) {
            SousActiviteImportDTO sa = sousActivites.get(j);
            String prefixe = "sous-activité #" + (j + 1);

            if (sa == null) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "]",
                        "La " + prefixe + " est un objet null."));
                continue;
            }

            if (estVide(sa.getDesignation())) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].designation",
                        "La désignation de la " + prefixe + " est obligatoire."));
            } else if (sa.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_SOUS) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].designation",
                        "La désignation de la " + prefixe + " dépasse "
                                + LONGUEUR_MAX_DESIGNATION_SOUS + " caractères."));
            }

            if (sa.getDateDebut() == null) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].dateDebut",
                        "La date de début prévue de la " + prefixe + " est obligatoire."));
            }
            if (sa.getDateFin() == null) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].dateFin",
                        "La date de fin prévue de la " + prefixe + " est obligatoire."));
            }
            if (sa.getDateDebut() != null && sa.getDateFin() != null
                    && sa.getDateFin().isBefore(sa.getDateDebut())) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].dateFin",
                        "La date de fin prévue de la " + prefixe
                                + " doit être supérieure ou égale à la date de début prévue."));
            }

            if (sa.getLivrables() != null) {
                for (int k = 0; k < sa.getLivrables().size(); k++) {
                    validerLivrable(idActivite, j, k, sa.getLivrables().get(k), erreurs);
                }
            }
        }
    }

    private void validerLivrable(
            String idActivite,
            int indexSous,
            int indexLivrable,
            LivrableImportDTO l,
            List<ErreurImportDTO> erreurs) {

        String champ = "sousActivites[" + indexSous + "].livrables[" + indexLivrable + "]";
        String libelle = "livrable #" + (indexLivrable + 1)
                + " de la sous-activité #" + (indexSous + 1);

        if (l == null) {
            erreurs.add(erreur(idActivite, champ, "Le " + libelle + " est un objet null."));
            return;
        }

        if (estVide(l.getDesignation())) {
            erreurs.add(erreur(idActivite, champ + ".designation",
                    "La désignation du " + libelle + " est obligatoire."));
        } else if (l.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_LIVRABLE) {
            erreurs.add(erreur(idActivite, champ + ".designation",
                    "La désignation du " + libelle + " dépasse "
                            + LONGUEUR_MAX_DESIGNATION_LIVRABLE + " caractères."));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Resultats intermediaires                                            */
    /* ------------------------------------------------------------------ */

    private void validerResultatsIntermediaires(
            String idActivite,
            List<ResultatIntermediaireImportDTO> resultats,
            List<ErreurImportDTO> erreurs) {

        if (resultats == null) {
            return;
        }

        for (int i = 0; i < resultats.size(); i++) {
            ResultatIntermediaireImportDTO r = resultats.get(i);
            if (r == null) {
                erreurs.add(erreur(idActivite, "resultatsIntermediaires[" + i + "]",
                        "Le résultat intermédiaire #" + (i + 1) + " est un objet null."));
                continue;
            }
            if (estVide(r.getDesignation())) {
                erreurs.add(erreur(idActivite, "resultatsIntermediaires[" + i + "].designation",
                        "La désignation du résultat intermédiaire #" + (i + 1) + " est obligatoire."));
            } else if (r.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_RESULTAT) {
                erreurs.add(erreur(idActivite, "resultatsIntermediaires[" + i + "].designation",
                        "La désignation du résultat intermédiaire #" + (i + 1)
                                + " dépasse " + LONGUEUR_MAX_DESIGNATION_RESULTAT + " caractères."));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Indicateurs                                                         */
    /* ------------------------------------------------------------------ */

    private void validerIndicateurs(
            String idActivite,
            List<IndicateurImportReferenceDTO> indicateurs,
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements) {

        if (indicateurs == null) {
            return;
        }

        Set<String> vus = new HashSet<>();
        for (int i = 0; i < indicateurs.size(); i++) {
            IndicateurImportReferenceDTO ind = indicateurs.get(i);
            if (ind == null) {
                erreurs.add(erreur(idActivite, "indicateurs[" + i + "]",
                        "L'indicateur #" + (i + 1) + " est un objet null."));
                continue;
            }
            if (estVide(ind.getDesignation())) {
                erreurs.add(erreur(idActivite, "indicateurs[" + i + "].designation",
                        "La désignation de l'indicateur #" + (i + 1) + " est obligatoire."));
                continue;
            }
            String cle = ind.getDesignation().trim().toLowerCase(Locale.ROOT);
            if (!vus.add(cle)) {
                erreurs.add(erreur(idActivite, "indicateurs[" + i + "].designation",
                        "L'indicateur « " + ind.getDesignation()
                                + " » est présent plusieurs fois sur la même activité."));
                continue;
            }
            long n = indicateurRepository.countByDesignationIgnoreCase(ind.getDesignation().trim());
            if (n == 0) {
                avertissements.add(avertissement(idActivite, "indicateurs[" + i + "].designation",
                        "L'indicateur « " + ind.getDesignation()
                                + " » sera créé automatiquement à l'import."));
            } else if (n > 1) {
                erreurs.add(erreur(idActivite, "indicateurs[" + i + "].designation",
                        "Plusieurs indicateurs portent la désignation « "
                                + ind.getDesignation() + " » : impossible de choisir."));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Apercu                                                              */
    /* ------------------------------------------------------------------ */

    private List<ApercuActiviteDTO> construireApercu(ActiviteImportRequest request) {
        String statutInitial = Boolean.TRUE.equals(request.getActivitesDejaValidees())
                ? StatutsActivite.NON_COMMENCEE
                : StatutsActivite.BROUILLON;

        List<ApercuActiviteDTO> apercu = new ArrayList<>();
        for (ActiviteImportDTO a : request.getActivites()) {
            int nbSous = a.getSousActivites() == null ? 0 : a.getSousActivites().size();
            int nbLivrables = 0;
            LocalDate dateDebut = null;
            LocalDate dateFin = null;

            if (a.getSousActivites() != null) {
                for (SousActiviteImportDTO sa : a.getSousActivites()) {
                    if (sa == null) continue;
                    if (sa.getLivrables() != null) {
                        nbLivrables += sa.getLivrables().size();
                    }
                    if (sa.getDateDebut() != null
                            && (dateDebut == null || sa.getDateDebut().isBefore(dateDebut))) {
                        dateDebut = sa.getDateDebut();
                    }
                    if (sa.getDateFin() != null
                            && (dateFin == null || sa.getDateFin().isAfter(dateFin))) {
                        dateFin = sa.getDateFin();
                    }
                }
            }

                // Le code fourni est affiché sans le recalculer ; un code absent
                // sera attribué par le service métier lors de l'import.
            apercu.add(ApercuActiviteDTO.builder()
                    .code(a.getCode())
                    .reference(null)
                    .designation(a.getDesignation())
                    .dateDebut(dateDebut)
                    .dateFin(dateFin)
                    .nombreSousActivites(nbSous)
                    .nombreLivrables(nbLivrables)
                    .statutInitial(statutInitial)
                    .build());
        }
        return apercu;
    }

    /* ------------------------------------------------------------------ */
    /* Rapport + helpers                                                   */
    /* ------------------------------------------------------------------ */

    private RapportValidationDTO buildRapport(
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements,
            int nombreActivites,
            List<ApercuActiviteDTO> apercu) {

        boolean valide = erreurs.isEmpty();
        String message;
        if (valide) {
            if (avertissements.isEmpty()) {
                message = "Toutes les données du lot sont valides et peuvent être importées.";
            } else {
                message = "Les données sont valides, "
                        + avertissements.size()
                        + " avertissement(s) ont été identifié(s).";
            }
        } else {
            message = "Des erreurs ont été détectées dans les données à importer.";
        }

        return RapportValidationDTO.builder()
                .valide(valide)
                .message(message)
                .nombreActivites(nombreActivites)
                .nombreErreurs(erreurs.size())
                .nombreAvertissements(avertissements.size())
                .erreurs(erreurs)
                .avertissements(avertissements)
                .apercu(apercu)
                .build();
    }

    private ErreurImportDTO erreur(String activite, String champ, String message) {
        return ErreurImportDTO.builder()
                .activite(activite)
                .champ(champ)
                .message(message)
                .build();
    }

    private AvertissementImportDTO avertissement(String activite, String champ, String message) {
        return AvertissementImportDTO.builder()
                .activite(activite)
                .champ(champ)
                .message(message)
                .build();
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}