package mg.bank.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.indicateur.AgregationIndicateurDTO;
import mg.bank.backend.dto.indicateur.AtteinteIndicateurDTO;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.dto.indicateur.PeriodeAgregeeDTO;
import mg.bank.backend.dto.indicateur.SuiviIndicateurDTO;
import mg.bank.backend.dto.indicateur.UtilisateurMinimalDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurRequest;
import mg.bank.backend.enums.MethodeAgregation;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Indicateur;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.model.ValeurIndicateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.IndicateurRepository;
import mg.bank.backend.repository.ValeurIndicateurRepository;
import mg.bank.backend.repository.projection.IndicateurDetailRow;
import mg.bank.backend.repository.projection.ValeurIndicateurRow;

@Service
@RequiredArgsConstructor
public class SuiviIndicateurService {

    private static final String STATUT_ATTEINT = "ATTEINT";
    private static final String STATUT_NON_ATTEINT = "NON_ATTEINT";
    private static final String STATUT_INDETERMINE = "INDETERMINE";

    private static final String TYPE_KPI = "KPI";
    private static final String TYPE_KRI = "KRI";

    private static final int TAILLE_PAGE_MAX = 100;

    private final IndicateurRepository indicateurRepository;
    private final ValeurIndicateurRepository valeurIndicateurRepository;
    private final ActiviteRepository activiteRepository;

    private final ActiviteService activiteService;

    // ------------------------------------------------------------------
    // F1 — Historique pagine
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ValeurIndicateurDTO> historique(
            Integer idIndicateur, int page, int size) {

        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);

        if (page < 0) {
            throw new ApiException("Le numéro de page ne peut pas être négatif",
                    HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > TAILLE_PAGE_MAX) {
            throw new ApiException(
                    "La taille de page doit être comprise entre 1 et " + TAILLE_PAGE_MAX,
                    HttpStatus.BAD_REQUEST);
        }

        List<ValeurIndicateur> toutes = valeurIndicateurRepository
                .findHistoriqueByIndicateur(indicateur.getIdIndicateur());

        Pageable pageable = PageRequest.of(page, size);
        int debut = Math.min(page * size, toutes.size());
        int fin = Math.min(debut + size, toutes.size());

        List<ValeurIndicateurDTO> contenu = toutes.subList(debut, fin).stream()
                .map(this::versValeurDto)
                .toList();

        return new PageImpl<>(contenu, pageable, toutes.size());
    }

    // ------------------------------------------------------------------
    // F2 — Saisie append-only
    // ------------------------------------------------------------------

    @Transactional
    public ValeurIndicateurDTO saisir(
            Integer idIndicateur, ValeurIndicateurRequest request) {

        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);

        if (!Boolean.TRUE.equals(indicateur.getActif())) {
            throw new ApiException(
                    "L'indicateur n'est pas actif : aucune saisie n'est possible.",
                    HttpStatus.CONFLICT);
        }

        // Coherence des dates
        if (request.getPeriodeFin() == null || request.getPeriodeDebut() == null) {
            throw new ApiException(
                    "Les périodes de début et de fin sont obligatoires.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!request.getPeriodeFin().isAfter(request.getPeriodeDebut())) {
            throw new ApiException(
                    "La période de fin doit être strictement postérieure à la "
                            + "période de début.",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.getValeur() == null) {
            throw new ApiException("La valeur est obligatoire.",
                    HttpStatus.BAD_REQUEST);
        }

        // Doublon de periode : la regle append-only n'accepte pas deux valeurs
        // sur la meme periode pour le meme indicateur.
        if (valeurIndicateurRepository
                .existsByIndicateur_IdIndicateurAndPeriodeDebutAndPeriodeFin(
                        indicateur.getIdIndicateur(),
                        request.getPeriodeDebut(),
                        request.getPeriodeFin())) {
            throw new ApiException(
                    "Une valeur existe déjà pour cette période sur cet indicateur.",
                    HttpStatus.CONFLICT);
        }

        Utilisateur auteur = activiteService.utilisateurCourant();

        ValeurIndicateur valeur = ValeurIndicateur.builder()
                .valeur(request.getValeur())
                .periodeDebut(request.getPeriodeDebut())
                .periodeFin(request.getPeriodeFin())
                .commentaire(request.getCommentaire())
                .dateSaisie(LocalDateTime.now())
                .utilisateur(auteur)
                .indicateur(indicateur)
                .build();

        return versValeurDto(valeurIndicateurRepository.save(valeur));
    }

    // ------------------------------------------------------------------
    // F3 — Valeur courante
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ValeurIndicateurDTO valeurCourante(Integer idIndicateur) {
        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);

        ValeurIndicateur courante = trouverValeurCourante(indicateur.getIdIndicateur());
        if (courante == null) {
            throw new ApiException(
                    "Aucune valeur n'a encore été saisie pour cet indicateur.",
                    HttpStatus.NOT_FOUND);
        }
        return versValeurDto(courante);
    }

    // ------------------------------------------------------------------
    // F4 — Atteinte
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AtteinteIndicateurDTO atteinte(Integer idIndicateur) {
        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);
        ValeurIndicateur courante = trouverValeurCourante(indicateur.getIdIndicateur());
        return versAtteinteDto(indicateur, courante);
    }

    // ------------------------------------------------------------------
    // F5 — Suivi d'une activite
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SuiviIndicateurDTO suiviActivite(Integer idActivite) {
        // Verifie le perimetre et l'existence de l'activite.
        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

        if (activiteRepository.findActiviteDetail(
                idActivite, servicePerimetre, departementPerimetre,
                String.join(",", mg.bank.backend.enums.StatutActiviteEnum.codesTerminaux()))
                .isEmpty()) {
            throw activiteService.refusOuAbsence(idActivite);
        }

        // Charge les indicateurs de l'activite via la projection existante.
        List<IndicateurDetailRow> indicateurs = activiteRepository.findIndicateurs(idActivite);

        // Charge toutes les valeurs des indicateurs de l'activite, en une requete.
        List<ValeurIndicateurRow> valeurs = activiteRepository.findValeursIndicateurs(idActivite);

        // Regroupe par idIndicateur, dans l'ordre deja trie (periode_fin DESC).
        Map<Integer, ValeurIndicateurRow> derniereParIndicateur = new HashMap<>();
        for (ValeurIndicateurRow v : valeurs) {
            derniereParIndicateur.putIfAbsent(v.getIdIndicateur(), v);
        }

        List<AtteinteIndicateurDTO> items = new ArrayList<>();
        for (IndicateurDetailRow row : indicateurs) {
            ValeurIndicateurRow derniere = derniereParIndicateur.get(row.getId());
            items.add(versAtteinteDto(row, derniere));
        }

        // Le libelle de l'activite pour le front.
        var detail = activiteRepository.findActiviteDetail(
                idActivite, servicePerimetre, departementPerimetre,
                String.join(",", mg.bank.backend.enums.StatutActiviteEnum.codesTerminaux()))
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        return SuiviIndicateurDTO.builder()
                .idActivite(detail.getId())
                .code(detail.getCode())
                .designation(detail.getDesignation())
                .indicateurs(items)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Indicateur chargerIndicateurDansPerimetre(Integer idIndicateur) {
        Indicateur indicateur = indicateurRepository.findById(idIndicateur)
                .orElseThrow(() -> new ApiException(
                        "Indicateur introuvable.", HttpStatus.NOT_FOUND));

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

        long n = indicateurRepository.compterDansPerimetre(
                idIndicateur, servicePerimetre, departementPerimetre);
        if (n == 0) {
            // On ne revele pas l'existence de l'indicateur si hors perimetre.
            throw new ApiException(
                    "Vous n'êtes pas autorisé à consulter cet indicateur.",
                    HttpStatus.FORBIDDEN);
        }
        return indicateur;
    }

    /**
     * Valeur courante : la derniere dont la periode est terminee ; a defaut,
     * la derniere saisie quelle que soit sa periode (pour qu'un indicateur
     * dont la premiere saisie est en cours d'execution ne soit pas "vide").
     */
    private ValeurIndicateur trouverValeurCourante(Integer idIndicateur) {
        List<ValeurIndicateur> terminees = valeurIndicateurRepository.findValeursTerminees(
                idIndicateur, LocalDateTime.now());
        if (!terminees.isEmpty()) {
            return terminees.get(0);
        }
        List<ValeurIndicateur> toutes = valeurIndicateurRepository.findToutesValeurs(idIndicateur);
        return toutes.isEmpty() ? null : toutes.get(0);
    }

    private ValeurIndicateurDTO versValeurDto(ValeurIndicateur v) {
        return ValeurIndicateurDTO.builder()
                .id(v.getIdValeurIndicateur())
                .valeur(v.getValeur())
                .periodeDebut(v.getPeriodeDebut())
                .periodeFin(v.getPeriodeFin())
                .commentaire(v.getCommentaire())
                .dateSaisie(v.getDateSaisie())
                .saisiePar(v.getUtilisateur() == null ? null
                        : UtilisateurMinimalDTO.builder()
                                .id(v.getUtilisateur().getIdUtilisateur())
                                .nom(v.getUtilisateur().getNom())
                                .prenom(v.getUtilisateur().getPrenom())
                                .build())
                .build();
    }

    private AtteinteIndicateurDTO versAtteinteDto(
            IndicateurDetailRow indicateur, ValeurIndicateurRow derniere) {

        BigDecimal cible = indicateur.getValeurCible();
        BigDecimal valeur = derniere == null ? null : derniere.getValeur();
        BigDecimal ecart = (valeur != null && cible != null)
                ? valeur.subtract(cible)
                : null;

        String statut;
        if (valeur == null || cible == null || indicateur.getTypeIndicateur() == null) {
            statut = STATUT_INDETERMINE;
        } else if (TYPE_KPI.equals(indicateur.getTypeIndicateur())) {
            statut = valeur.compareTo(cible) >= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else if (TYPE_KRI.equals(indicateur.getTypeIndicateur())) {
            statut = valeur.compareTo(cible) <= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else {
            statut = STATUT_INDETERMINE;
        }

        return AtteinteIndicateurDTO.builder()
                .idIndicateur(indicateur.getId())
                .code(indicateur.getCode())
                .designation(indicateur.getDesignation())
                .typeIndicateur(indicateur.getTypeIndicateur())
                .uniteMesure(indicateur.getUniteMesure())
                .valeurCible(cible)
                .seuilMin(indicateur.getSeuilMin())
                .seuilMax(indicateur.getSeuilMax())
                .valeurCourante(valeur)
                .periodeDebut(derniere == null ? null : derniere.getPeriodeDebut())
                .periodeFin(derniere == null ? null : derniere.getPeriodeFin())
                .ecart(ecart)
                .statut(statut)
                .build();
    }

    private AtteinteIndicateurDTO versAtteinteDto(Indicateur indicateur, ValeurIndicateur derniere) {
        BigDecimal cible = indicateur.getValeurCible();
        BigDecimal valeur = derniere == null ? null : derniere.getValeur();
        BigDecimal ecart = (valeur != null && cible != null)
                ? valeur.subtract(cible)
                : null;

        String statut;
        if (valeur == null || cible == null || indicateur.getTypeIndicateur() == null) {
            statut = STATUT_INDETERMINE;
        } else if (TYPE_KPI.equals(indicateur.getTypeIndicateur())) {
            statut = valeur.compareTo(cible) >= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else if (TYPE_KRI.equals(indicateur.getTypeIndicateur())) {
            statut = valeur.compareTo(cible) <= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else {
            statut = STATUT_INDETERMINE;
        }

        return AtteinteIndicateurDTO.builder()
                .idIndicateur(indicateur.getIdIndicateur())
                .code(indicateur.getCode())
                .designation(indicateur.getDesignation())
                .typeIndicateur(indicateur.getTypeIndicateur())
                .uniteMesure(indicateur.getUniteMesure())
                .valeurCible(cible)
                .seuilMin(indicateur.getSeuilMin())
                .seuilMax(indicateur.getSeuilMax())
                .valeurCourante(valeur)
                .periodeDebut(derniere == null ? null : derniere.getPeriodeDebut())
                .periodeFin(derniere == null ? null : derniere.getPeriodeFin())
                .ecart(ecart)
                .statut(statut)
                .build();
    }

    // ------------------------------------------------------------------
    // F6 — Agrégation sur une plage, granularité automatique
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AgregationIndicateurDTO agreger(
            Integer idIndicateur,
            LocalDateTime debut,
            LocalDateTime fin,
            MethodeAgregation methode) {

        if (debut == null || fin == null) {
            throw new ApiException("Les bornes de la période sont obligatoires.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!fin.isAfter(debut)) {
            throw new ApiException(
                    "La fin de période doit être strictement postérieure au début.",
                    HttpStatus.BAD_REQUEST);
        }
        if (methode == null) {
            throw new ApiException("La méthode d'agrégation est obligatoire.",
                    HttpStatus.BAD_REQUEST);
        }

        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);

        String freq = indicateur.getFrequenceAggregation();
        if (freq == null || freq.isBlank()) {
            throw new ApiException(
                    "Aucune fréquence d'agrégation n'est définie sur cet indicateur.",
                    HttpStatus.CONFLICT);
        }

        List<ValeurIndicateur> toutes = valeurIndicateurRepository.findToutesValeurs(idIndicateur);
        List<ValeurIndicateur> dansPlage = toutes.stream()
                .filter(v -> !v.getPeriodeFin().isBefore(debut)
                        && !v.getPeriodeDebut().isAfter(fin))
                .toList();

        // Découpage de la plage selon la fréquence de l'indicateur.
        List<PeriodeAgregeeDTO> periodes = decouperEtAgreger(dansPlage, debut, fin, freq, methode);

        // Agrégation globale sur toute la plage (même méthode).
        BigDecimal globale = calculer(
                dansPlage.stream().map(ValeurIndicateur::getValeur).toList(), methode);

        return AgregationIndicateurDTO.builder()
                .idIndicateur(indicateur.getIdIndicateur())
                .code(indicateur.getCode())
                .designation(indicateur.getDesignation())
                .uniteMesure(indicateur.getUniteMesure())
                .typeIndicateur(indicateur.getTypeIndicateur())
                .frequenceAggregation(freq)
                .methode(methode.name())
                .debut(debut)
                .fin(fin)
                .valeurGlobale(globale)
                .nombreSaisiesTotal(dansPlage.size())
                .periodes(periodes)
                .build();
    }

    /**
     * Découpe [debut, fin] en sous-périodes selon la fréquence, et agrège les
     * valeurs de chaque sous-période avec la méthode demandée.
     */
    private List<PeriodeAgregeeDTO> decouperEtAgreger(
            List<ValeurIndicateur> valeurs,
            LocalDateTime debut,
            LocalDateTime fin,
            String frequence,
            MethodeAgregation methode) {

        String freq = frequence.trim().toUpperCase(java.util.Locale.ROOT);

        List<PeriodeAgregeeDTO> resultat = new ArrayList<>();

        switch (freq) {
            case "MENSUEL" -> {
                LocalDate curseur = debut.toLocalDate().withDayOfMonth(1);
                LocalDate borneFin = fin.toLocalDate().withDayOfMonth(1);
                while (!curseur.isAfter(borneFin)) {
                    LocalDate pDebut = curseur;
                    LocalDate pFin = curseur.withDayOfMonth(curseur.lengthOfMonth());
                    resultat.add(construirePeriode(
                            valeurs, pDebut.atStartOfDay(), pFin.atTime(23, 59, 59),
                            libelleMois(curseur), methode));
                    curseur = curseur.plusMonths(1);
                }
            }
            case "TRIMESTRIEL" -> {
                LocalDate curseur = debut.toLocalDate().withDayOfMonth(1);
                // Ramene au debut du trimestre
                curseur = curseur.withMonth(((curseur.getMonthValue() - 1) / 3) * 3 + 1);
                LocalDate borneFin = fin.toLocalDate();
                while (!curseur.isAfter(borneFin)) {
                    LocalDate pDebut = curseur;
                    LocalDate pFin = curseur.plusMonths(3).minusDays(1);
                    resultat.add(construirePeriode(
                            valeurs, pDebut.atStartOfDay(), pFin.atTime(23, 59, 59),
                            libelleTrimestre(pDebut), methode));
                    curseur = curseur.plusMonths(3);
                }
            }
            case "SEMESTRIEL" -> {
                LocalDate curseur = debut.toLocalDate().withDayOfMonth(1);
                curseur = curseur.withMonth(curseur.getMonthValue() <= 6 ? 1 : 7);
                LocalDate borneFin = fin.toLocalDate();
                while (!curseur.isAfter(borneFin)) {
                    LocalDate pDebut = curseur;
                    LocalDate pFin = curseur.plusMonths(6).minusDays(1);
                    resultat.add(construirePeriode(
                            valeurs, pDebut.atStartOfDay(), pFin.atTime(23, 59, 59),
                            libelleSemestre(pDebut), methode));
                    curseur = curseur.plusMonths(6);
                }
            }
            case "ANNUEL" -> {
                int anneeDebut = debut.getYear();
                int anneeFin = fin.getYear();
                for (int a = anneeDebut; a <= anneeFin; a++) {
                    LocalDate pDebut = LocalDate.of(a, 1, 1);
                    LocalDate pFin = LocalDate.of(a, 12, 31);
                    resultat.add(construirePeriode(
                            valeurs, pDebut.atStartOfDay(), pFin.atTime(23, 59, 59),
                            String.valueOf(a), methode));
                }
            }
            default -> throw new ApiException(
                    "Fréquence d'agrégation non supportée : « " + frequence + " ».",
                    HttpStatus.BAD_REQUEST);
        }

        return resultat;
    }

    private PeriodeAgregeeDTO construirePeriode(
            List<ValeurIndicateur> valeurs,
            LocalDateTime debut,
            LocalDateTime fin,
            String libelle,
            MethodeAgregation methode) {

        List<BigDecimal> dansPeriode = valeurs.stream()
                .filter(v -> !v.getPeriodeFin().isBefore(debut)
                        && !v.getPeriodeDebut().isAfter(fin))
                .map(ValeurIndicateur::getValeur)
                .toList();

        return PeriodeAgregeeDTO.builder()
                .libelle(libelle)
                .debut(debut)
                .fin(fin)
                .valeurAgregee(calculer(dansPeriode, methode))
                .nombreSaisies(dansPeriode.size())
                .build();
    }

    private BigDecimal calculer(List<BigDecimal> valeurs, MethodeAgregation methode) {
        if (valeurs == null || valeurs.isEmpty()) {
            return null;
        }
        return switch (methode) {
            case MOYENNE -> {
                BigDecimal somme = valeurs.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield somme.divide(BigDecimal.valueOf(valeurs.size()), 2, java.math.RoundingMode.HALF_UP);
            }
            case SOMME -> valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case MIN -> valeurs.stream().min(BigDecimal::compareTo).orElse(null);
            case MAX -> valeurs.stream().max(BigDecimal::compareTo).orElse(null);
            case DERNIERE -> valeurs.get(valeurs.size() - 1); // hypothèse : deja triées periodeFin DESC
        };
    }

    private String libelleMois(LocalDate d) {
        return d.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH)
                + " " + d.getYear();
    }

    private String libelleTrimestre(LocalDate d) {
        int t = ((d.getMonthValue() - 1) / 3) + 1;
        return "T" + t + " " + d.getYear();
    }

    private String libelleSemestre(LocalDate d) {
        int s = d.getMonthValue() <= 6 ? 1 : 2;
        return "S" + s + " " + d.getYear();
    }

    // ------------------------------------------------------------------
    // F12 — Indicateurs en retard de saisie
    // ------------------------------------------------------------------

    private static final String STATUT_EN_RETARD = "EN_RETARD";
    private static final String STATUT_JAMAIS_SAISI = "JAMAIS_SAISI";

    /**
     * Liste les indicateurs dont la derniere saisie depasse leur frequence
     * de verification. Deux cas :
     * - EN_RETARD : au moins une valeur saisie, mais derniere periode
     * plus ancienne que la cadence ;
     * - JAMAIS_SAISI : aucune valeur pour cet indicateur.
     *
     * <p>
     * Filtres : renvoie uniquement les deux statuts d'alerte par defaut.
     * Un parametre {@code inclureAJour} et {@code inclureJamaisSaisi} permet
     * d'elargir la reponse.
     * </p>
     */
    @Transactional(readOnly = true)
    public List<IndicateurEnRetardDTO> listerEnRetard(
            boolean inclureAJour, boolean inclureJamaisSaisi) {

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

        List<Indicateur> candidats = indicateurRepository.findActifsAvecFrequenceDansPerimetre(
                servicePerimetre, departementPerimetre);

        List<IndicateurEnRetardDTO> resultat = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();

        for (Indicateur indicateur : candidats) {
            List<ValeurIndicateur> valeurs = valeurIndicateurRepository
                    .findToutesValeurs(indicateur.getIdIndicateur());

            if (valeurs.isEmpty()) {
                if (inclureJamaisSaisi) {
                    resultat.add(IndicateurEnRetardDTO.builder()
                            .idIndicateur(indicateur.getIdIndicateur())
                            .code(indicateur.getCode())
                            .designation(indicateur.getDesignation())
                            .typeIndicateur(indicateur.getTypeIndicateur())
                            .frequenceVerification(indicateur.getFrequenceVerification())
                            .dernierePeriodeFin(null)
                            .prochaineEcheance(null)
                            .joursDeRetard(null)
                            .statut(STATUT_JAMAIS_SAISI)
                            .build());
                }
                continue;
            }

            // La liste est triee periodeFin DESC, donc le premier element est le plus
            // recent.
            ValeurIndicateur derniere = valeurs.get(0);
            LocalDateTime prochaineEcheance = calculerProchaineEcheance(
                    derniere.getPeriodeFin(), indicateur.getFrequenceVerification());

            if (prochaineEcheance == null) {
                continue; // frequence non reconnue -> ignore
            }

            boolean enRetard = maintenant.isAfter(prochaineEcheance);
            if (!enRetard && !inclureAJour) {
                continue;
            }

            long jours = enRetard
                    ? java.time.temporal.ChronoUnit.DAYS.between(prochaineEcheance, maintenant)
                    : 0L;

            resultat.add(IndicateurEnRetardDTO.builder()
                    .idIndicateur(indicateur.getIdIndicateur())
                    .code(indicateur.getCode())
                    .designation(indicateur.getDesignation())
                    .typeIndicateur(indicateur.getTypeIndicateur())
                    .frequenceVerification(indicateur.getFrequenceVerification())
                    .dernierePeriodeFin(derniere.getPeriodeFin())
                    .prochaineEcheance(prochaineEcheance)
                    .joursDeRetard(jours)
                    .statut(enRetard ? STATUT_EN_RETARD : "A_JOUR")
                    .build());
        }

        return resultat;
    }

    /**
     * Ajoute `frequence` a `base` selon la cadence. Retourne null si la
     * frequence n'est pas reconnue.
     */
    private LocalDateTime calculerProchaineEcheance(LocalDateTime base, String frequence) {
        if (base == null || frequence == null) {
            return null;
        }
        return switch (frequence.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "MENSUEL" -> base.plusMonths(1);
            case "TRIMESTRIEL" -> base.plusMonths(3);
            case "SEMESTRIEL" -> base.plusMonths(6);
            case "ANNUEL" -> base.plusYears(1);
            default -> null;
        };
    }
}