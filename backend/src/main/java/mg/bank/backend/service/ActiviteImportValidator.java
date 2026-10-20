package mg.bank.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.importactivite.ActiviteImportDTO;
import mg.bank.backend.dto.importactivite.ActiviteImportRequest;
import mg.bank.backend.dto.importactivite.ApercuActiviteDTO;
import mg.bank.backend.dto.importactivite.AvertissementImportDTO;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportReferenceDTO;
import mg.bank.backend.dto.importactivite.LivrableImportDTO;
import mg.bank.backend.dto.importactivite.ObjectifSpecifiqueImportReferenceDTO;
import mg.bank.backend.dto.importactivite.OrigineImportReferenceDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportDTO;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.model.ObjectifSpecifique;
import mg.bank.backend.model.Procedure;
import mg.bank.backend.model.Service;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.IndicateurRepository;
import mg.bank.backend.repository.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.OrigineRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.ProcedureRepository;
import mg.bank.backend.repository.ServiceRepository;
import mg.bank.backend.repository.SiteRepository;
import mg.bank.backend.repository.SousActiviteRepository;
import mg.bank.backend.repository.TypeActiviteRepository;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ActiviteImportValidator {

    // ------------------------------------------------------------------
    // Constantes
    // ------------------------------------------------------------------

    private static final String VERSION_ATTENDUE = "1.0";

    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 2100;

    private static final int LONGUEUR_MAX_CODE = 20;
    private static final int LONGUEUR_MAX_REFERENCE = 20;
    private static final int LONGUEUR_MAX_DESIGNATION_ACTIVITE = 255;
    private static final int LONGUEUR_MAX_DESIGNATION_SOUS = 255;
    private static final int LONGUEUR_MAX_DESIGNATION_LIVRABLE = 250;
    private static final int LONGUEUR_MAX_DESIGNATION_RESULTAT = 255;
    private static final int LONGUEUR_MAX_DESIGNATION_INDICATEUR = 250;
    private static final int LONGUEUR_MAX_DESIGNATION_ORIGINE = 255;

    // ------------------------------------------------------------------
    // Dependances
    // ------------------------------------------------------------------

    private final ActiviteService activiteService;

    private final ActiviteRepository activiteRepository;
    private final SousActiviteRepository sousActiviteRepository;
    private final ServiceRepository serviceRepository;
    private final ProcedureRepository procedureRepository;
    private final PrioriteRepository prioriteRepository;
    private final TypeActiviteRepository typeActiviteRepository;
    private final SiteRepository siteRepository;
    private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
    private final IndicateurRepository indicateurRepository;
    private final OrigineRepository origineRepository;

    // ------------------------------------------------------------------
    // Point d'entree
    // ------------------------------------------------------------------

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

            Set<String> codesActivitesVus = new HashSet<>();
            Set<String> codesSousActivitesVus = new HashSet<>();

            Map<String, Integer> designationsVues = new HashMap<>();

            for (int i = 0; i < request.getActivites().size(); i++) {
                validerActivite(
                        request.getActivites().get(i),
                        i + 1,
                        request.getTypeImport(),
                        request.getAnnee(),
                        request.getActivitesDejaValidees(),
                        codesActivitesVus,
                        codesSousActivitesVus,
                        designationsVues,
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
                // Si les activites sont non validees, procedure obligatoire :
                // l'apercu n'est propose que si elle est resolue.
                && (Boolean.TRUE.equals(request.getActivitesDejaValidees())
                        || procedureRacine != null)) {
            apercu = construireApercu(request);
        }

        return buildRapport(erreurs, avertissements, nombreActivites, apercu);
    }

    // ------------------------------------------------------------------
    // Entete
    // ------------------------------------------------------------------

    private void validerEntete(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (request == null) {
            erreurs.add(erreur(null, null, "Le corps de la requête d'import est vide."));
            return;
        }

        // version
        if (estVide(request.getVersion())) {
            erreurs.add(erreur(null, "version",
                    "La version du format d'import est obligatoire."));
        } else if (!VERSION_ATTENDUE.equals(request.getVersion().trim())) {
            erreurs.add(erreur(null, "version",
                    "Version d'import non supportée : « " + request.getVersion()
                            + " ». Version attendue : « " + VERSION_ATTENDUE + " »."));
        }

        // service (identifiant, resolu juste apres)
        if (request.getService() == null) {
            erreurs.add(erreur(null, "service",
                    "Le service est obligatoire pour l'import."));
        }

        // annee
        if (request.getAnnee() == null) {
            erreurs.add(erreur(null, "annee",
                    "L'année de l'import est obligatoire."));
        } else if (request.getAnnee() < ANNEE_MIN || request.getAnnee() > ANNEE_MAX) {
            erreurs.add(erreur(null, "annee",
                    "L'année doit être comprise entre " + ANNEE_MIN
                            + " et " + ANNEE_MAX + "."));
        }

        // procedure : obligatoire si activitesDejaValidees = false
        if (Boolean.FALSE.equals(request.getActivitesDejaValidees())
                && request.getProcedure() == null) {
            erreurs.add(erreur(null, "procedure",
                    "La procédure est obligatoire pour des activités non encore validées."));
        }

        // typeImport : enum ; Jackson rejette deja les valeurs inconnues
        // avant d'arriver ici. Seule l'absence est a signaler.
        if (request.getTypeImport() == null) {
            erreurs.add(erreur(null, "typeImport",
                    "Le type d'import est obligatoire (PTA ou NON_PTA)."));
        }

        // activitesDejaValidees
        if (request.getActivitesDejaValidees() == null) {
            erreurs.add(erreur(null, "activitesDejaValidees",
                    "Le paramètre « activitesDejaValidees » est obligatoire."));
        }

        // activites
        if (request.getActivites() == null) {
            erreurs.add(erreur(null, "activites",
                    "Le tableau « activites » est obligatoire."));
        } else if (request.getActivites().isEmpty()) {
            erreurs.add(erreur(null, "activites",
                    "Le lot d'import doit contenir au moins une activité."));
        }
    }

    // ------------------------------------------------------------------
    // Service et procedure racine
    // ------------------------------------------------------------------

    private Service resoudreServiceRacine(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (request.getService() == null) {
            return null; // deja signale dans validerEntete
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

    /**
     * Resolution de la procedure racine.
     *
     * <p>
     * Uniquement si {@code activitesDejaValidees = false}. Si
     * {@code activitesDejaValidees = true}, la procedure est ignoree : le
     * statut initial est NON_COMMENCE et aucune procedure n'est associee.
     * </p>
     */
    private Procedure resoudreProcedureRacine(ActiviteImportRequest request, List<ErreurImportDTO> erreurs) {
        if (Boolean.TRUE.equals(request.getActivitesDejaValidees())) {
            return null; // ignoree volontairement
        }
        if (request.getProcedure() == null) {
            return null; // deja signale dans validerEntete
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
        if (perimetre == null) {
            return false;
        }
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

    // ------------------------------------------------------------------
    // Activite
    // ------------------------------------------------------------------

    private void validerActivite(
            ActiviteImportDTO a,
            int index,
            TypeImport typeImport,
            Integer annee,
            Boolean activitesDejaValidees,
            Set<String> codesActivitesVus,
            Set<String> codesSousActivitesVus,
            Map<String, Integer> designationsVues,
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

        // --- Date de debut prevue ---
        if (a.getDateDebut() == null) {
            erreurs.add(erreur(id, "dateDebut",
                    "La date de début prévue est obligatoire."));
        }

        // --- Coherence des dates prevues ---
        if (a.getDateDebut() != null && a.getDateFin() != null
                && a.getDateFin().isBefore(a.getDateDebut())) {
            erreurs.add(erreur(id, "dateFin",
                    "La date de fin doit être supérieure ou égale à la date de début."));
        }

        if (annee != null && a.getDateDebut() != null
                && a.getDateDebut().getYear() != annee) {
            erreurs.add(erreur(id, "dateDebut",
                    "La date de début prévue (" + a.getDateDebut()
                            + ") doit appartenir à l'année de l'import (" + annee + ")."));
        }

        // --- Coherence des dates reelles ---
        if (a.getDateDebutReelle() != null && a.getDateFinReelle() != null
                && a.getDateFinReelle().isBefore(a.getDateDebutReelle())) {
            erreurs.add(erreur(id, "dateFinReelle",
                    "La date de fin réelle doit être supérieure ou égale à la date de début réelle."));
        }

        // --- Code (facultatif, genere s'il est absent) ---
        boolean estPta = TypeImport.PTA == typeImport;

        // Aucune regle de generation n'existe pour PTA : on refuse ici,
        // a l'etape /valider, plutot que de laisser /confirmer echouer en 400.
        if (estPta && estVide(a.getCode())) {
            erreurs.add(erreur(id, "code",
                    "Le code est obligatoire pour une activité PTA "
                            + "(aucune règle de génération n'est définie)."));
        }

        String code = normaliser(a.getCode());
        if (code != null) {
            if (code.length() > LONGUEUR_MAX_CODE) {
                erreurs.add(erreur(id, "code",
                        "Le code de l'activité dépasse " + LONGUEUR_MAX_CODE + " caractères."));
            }
            String cle = code.toLowerCase(Locale.ROOT);
            if (!codesActivitesVus.add(cle)) {
                erreurs.add(erreur(id, "code",
                        "Le code « " + code + " » est présent plusieurs fois dans le lot."));
            }
            if (activiteRepository.existsByCodeIgnoreCase(code)) {
                erreurs.add(erreur(id, "code",
                        "Le code « " + code + " » existe déjà en base."));
            }
        }

        // --- Reference (PTA : obligatoire ; NON_PTA : facultative) ---
        if (estPta && estVide(a.getReference())) {
            erreurs.add(erreur(id, "reference",
                    "La référence de l'activité est obligatoire pour un import PTA."));
        } else if (!estVide(a.getReference())
                && a.getReference().trim().length() > LONGUEUR_MAX_REFERENCE) {
            erreurs.add(erreur(id, "reference",
                    "La référence de l'activité dépasse "
                            + LONGUEUR_MAX_REFERENCE + " caractères."));
        }

        // --- Objectif specifique ---
        if (estPta) {
            verifierObjectifSpecifiquePta(
                    id,
                    a.getReference(),
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

        // --- Priorite (obligatoire) ---
        if (estVide(a.getPriorite())) {
            erreurs.add(erreur(id, "priorite",
                    "La priorité est obligatoire."));
        } else if (!prioriteRepository.existsByCodeIgnoreCase(a.getPriorite().trim())) {
            erreurs.add(erreur(id, "priorite",
                    "La priorité « " + a.getPriorite() + " » est introuvable."));
        }

        // --- Type d'activite (facultatif) ---
        if (!estVide(a.getTypeActivite())) {
            long n = typeActiviteRepository.countByDesignationIgnoreCase(a.getTypeActivite().trim());
            if (n == 0) {
                erreurs.add(erreur(id, "typeActivite",
                        "Le type d'activité « " + a.getTypeActivite() + " » est introuvable."));
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

        // --- Sous-activites ---
        validerSousActivites(id, a.getSousActivites(), activitesDejaValidees,
                codesSousActivitesVus, erreurs);

        // --- Resultats intermediaires ---
        validerResultatsIntermediaires(id, a.getResultatsIntermediaires(), erreurs);

        // --- Indicateurs ---
        validerIndicateurs(id, a.getIndicateurs(), erreurs, avertissements);

        // --- Origines ---
        validerOrigines(id, a.getOrigines(), erreurs, avertissements);
    }

    private String identifiantActivite(ActiviteImportDTO a, int index) {
        if (a == null) {
            return "activité #" + index;
        }
        if (!estVide(a.getReference())) {
            return a.getReference().trim();
        }
        if (!estVide(a.getCode())) {
            return a.getCode().trim();
        }
        return "activité #" + index;
    }

    // ------------------------------------------------------------------
    // Objectif specifique
    // ------------------------------------------------------------------
    private void verifierObjectifSpecifiquePta(
            String idActivite,
            String reference,
            Integer annee,
            ObjectifSpecifiqueImportReferenceDTO os,
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements) {

        // 1. Presence minimale
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

        // 2. Coherence reference / objectifCode
        if (!estVide(reference)) {
            String prefixeAttendu = os.getCode().trim() + "_";
            if (!reference.trim().startsWith(prefixeAttendu)) {
                erreurs.add(erreur(idActivite, "reference",
                        "La référence « " + reference
                                + " » doit commencer par « " + prefixeAttendu
                                + " » pour être rattachée à l'objectif « "
                                + os.getCode() + " »."));
            }
        }

        // 3. Objectif existant ?
        ObjectifSpecifique existant = objectifSpecifiqueRepository
                .findByCodeIgnoreCase(os.getCode().trim())
                .orElse(null);

        if (existant == null) {
            // Objectif absent : il sera cree a l'import. Avertissement
            // non bloquant, sous reserve que la designation soit fournie.
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

        // 4. Objectif existant : la designation fournie doit correspondre.
        if (!estVide(os.getDesignation())
                && !os.getDesignation().trim().equalsIgnoreCase(existant.getDesignation())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique.designation",
                    "La désignation fournie pour l'objectif « " + os.getCode()
                            + " » ne correspond pas à celle de la base (attendu : « "
                            + existant.getDesignation() + " »)."));
        }

        // 5. Objectif existant : l'annee doit correspondre a celle du lot.
        if (annee != null && !annee.equals(existant.getAnnee())) {
            erreurs.add(erreur(idActivite, "objectifSpecifique",
                    "L'objectif spécifique « " + os.getCode()
                            + " » existe déjà pour l'année " + existant.getAnnee()
                            + " ; l'import est configuré pour l'année " + annee + "."));
        }
    }

    // ------------------------------------------------------------------
    // Sous-activites et livrables
    // ------------------------------------------------------------------

    private void validerSousActivites(
            String idActivite,
            List<SousActiviteImportDTO> sousActivites,
            Boolean activitesDejaValidees,
            Set<String> codesVus,
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
            if (sa.getDateDebutReelle() != null && sa.getDateFinReelle() != null
                    && sa.getDateFinReelle().isBefore(sa.getDateDebutReelle())) {
                erreurs.add(erreur(idActivite, "sousActivites[" + j + "].dateFinReelle",
                        "La date de fin réelle de la " + prefixe
                                + " doit être supérieure ou égale à la date de début réelle."));
            }

            // Les dates reelles relevent du suivi : elles ne sont acceptees
            // que pour des activites deja validees (meme regle que le
            // service d'import).
            if (Boolean.FALSE.equals(activitesDejaValidees)
                    && (sa.getDateDebutReelle() != null || sa.getDateFinReelle() != null)) {
                erreurs.add(erreur(idActivite,
                        "sousActivites[" + j + "].dateDebutReelle",
                        "Les dates réelles de la " + prefixe
                                + " ne peuvent être fournies que pour des activités "
                                + "déjà validées."));
            }

            String code = normaliser(sa.getCode());
            if (code != null) {
                if (code.length() > LONGUEUR_MAX_CODE) {
                    erreurs.add(erreur(idActivite, "sousActivites[" + j + "].code",
                            "Le code de la " + prefixe + " dépasse "
                                    + LONGUEUR_MAX_CODE + " caractères."));
                }
                String cle = code.toLowerCase(Locale.ROOT);
                if (!codesVus.add(cle)) {
                    erreurs.add(erreur(idActivite, "sousActivites[" + j + "].code",
                            "Le code « " + code + " » de la " + prefixe
                                    + " est présent plusieurs fois dans le lot."));
                }
                if (sousActiviteRepository.existsByCodeIgnoreCase(code)) {
                    erreurs.add(erreur(idActivite, "sousActivites[" + j + "].code",
                            "Le code « " + code + " » de la " + prefixe
                                    + " existe déjà en base."));
                }
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

    // ------------------------------------------------------------------
    // Resultats intermediaires
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Indicateurs et origines
    // ------------------------------------------------------------------

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
            if (ind.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_INDICATEUR) {
                erreurs.add(erreur(idActivite, "indicateurs[" + i + "].designation",
                        "La désignation de l'indicateur #" + (i + 1) + " dépasse "
                                + LONGUEUR_MAX_DESIGNATION_INDICATEUR + " caractères."));
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

    private void validerOrigines(
            String idActivite,
            List<OrigineImportReferenceDTO> origines,
            List<ErreurImportDTO> erreurs,
            List<AvertissementImportDTO> avertissements) {

        if (origines == null) {
            return;
        }

        Set<String> vus = new HashSet<>();
        for (int i = 0; i < origines.size(); i++) {
            OrigineImportReferenceDTO o = origines.get(i);
            if (o == null) {
                erreurs.add(erreur(idActivite, "origines[" + i + "]",
                        "L'origine #" + (i + 1) + " est un objet null."));
                continue;
            }
            if (estVide(o.getDesignation())) {
                erreurs.add(erreur(idActivite, "origines[" + i + "].designation",
                        "La désignation de l'origine #" + (i + 1) + " est obligatoire."));
                continue;
            }
            if (o.getDesignation().length() > LONGUEUR_MAX_DESIGNATION_ORIGINE) {
                erreurs.add(erreur(idActivite, "origines[" + i + "].designation",
                        "La désignation de l'origine #" + (i + 1) + " dépasse "
                                + LONGUEUR_MAX_DESIGNATION_ORIGINE + " caractères."));
                continue;
            }
            String cle = o.getDesignation().trim().toLowerCase(Locale.ROOT);
            if (!vus.add(cle)) {
                erreurs.add(erreur(idActivite, "origines[" + i + "].designation",
                        "L'origine « " + o.getDesignation()
                                + " » est présente plusieurs fois sur la même activité."));
                continue;
            }
            long n = origineRepository.countByDesignationIgnoreCase(o.getDesignation().trim());
            if (n == 0) {
                avertissements.add(avertissement(idActivite, "origines[" + i + "].designation",
                        "L'origine « " + o.getDesignation()
                                + " » sera créée automatiquement à l'import."));
            } else if (n > 1) {
                erreurs.add(erreur(idActivite, "origines[" + i + "].designation",
                        "Plusieurs origines portent la désignation « "
                                + o.getDesignation() + " » : impossible de choisir."));
            }
        }
    }

    // ------------------------------------------------------------------
    // Apercu
    // ------------------------------------------------------------------

    private List<ApercuActiviteDTO> construireApercu(ActiviteImportRequest request) {
        String statutInitial = Boolean.TRUE.equals(request.getActivitesDejaValidees())
                ? StatutsActivite.NON_COMMENCEE
                : StatutsActivite.BROUILLON;

        List<ApercuActiviteDTO> apercu = new ArrayList<>();
        for (ActiviteImportDTO a : request.getActivites()) {
            int nbSous = a.getSousActivites() == null ? 0 : a.getSousActivites().size();
            int nbLivrables = 0;
            if (a.getSousActivites() != null) {
                for (SousActiviteImportDTO sa : a.getSousActivites()) {
                    if (sa != null && sa.getLivrables() != null) {
                        nbLivrables += sa.getLivrables().size();
                    }
                }
            }
            apercu.add(ApercuActiviteDTO.builder()
                    .code(a.getCode())
                    .reference(a.getReference())
                    .designation(a.getDesignation())
                    .dateDebut(a.getDateDebut())
                    .dateFin(a.getDateFin())
                    .nombreSousActivites(nbSous)
                    .nombreLivrables(nbLivrables)
                    .statutInitial(statutInitial)
                    .build());
        }
        return apercu;
    }

    // ------------------------------------------------------------------
    // Rapport
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    private String normaliser(String s) {
        return estVide(s) ? null : s.trim();
    }
}