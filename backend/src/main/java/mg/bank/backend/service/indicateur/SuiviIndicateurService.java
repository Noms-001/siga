package mg.bank.backend.service.indicateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.indicateur.AgregationIndicateurDTO;
import mg.bank.backend.dto.indicateur.AtteinteIndicateurDTO;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.dto.indicateur.PeriodeAgregeeDTO;
import mg.bank.backend.dto.indicateur.SuiviIndicateurDTO;
import mg.bank.backend.dto.indicateur.UtilisateurMinimalDTO;
import mg.bank.backend.dto.indicateur.ValeurSuiviDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurRequest;
import mg.bank.backend.dto.indicateur.CalendrierIndicateurDTO;
import mg.bank.backend.dto.indicateur.DashboardIndicateurDTO;
import mg.bank.backend.dto.indicateur.PeriodeCalendrierDTO;
import mg.bank.backend.enums.MethodeAgregation;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.indicateur.Indicateur;
import mg.bank.backend.model.indicateur.ValeurIndicateur;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;
import mg.bank.backend.repository.indicateur.ValeurIndicateurRepository;
import mg.bank.backend.repository.projection.indicateur.IndicateurDetailRow;
import mg.bank.backend.repository.projection.indicateur.ValeurIndicateurRow;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.utilisateur.CurrentUserService;

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
    private final CurrentUserService currentUserService;

    // ------------------------------------------------------------------
    // F1 — Historique pagine
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ValeurSuiviDTO> historique(
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

        List<ValeurSuiviDTO> contenu = toutes.subList(debut, fin).stream()
                .map(this::versValeurDto)
                .toList();

        return new PageImpl<>(contenu, pageable, toutes.size());
    }

    // ------------------------------------------------------------------
    // F2 — Saisie append-only
    // ------------------------------------------------------------------

    @Transactional
    public ValeurSuiviDTO saisir(
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

        Utilisateur auteur = currentUserService.get();

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
    public ValeurSuiviDTO valeurCourante(Integer idIndicateur) {
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
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        Integer servicePerimetre = perimetre.getFiltreService();
        Integer departementPerimetre = perimetre.getFiltreDepartement();

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

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        Integer servicePerimetre = perimetre.getFiltreService();
        Integer departementPerimetre = perimetre.getFiltreDepartement();

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

    private ValeurIndicateur trouverValeurCourante(Integer idIndicateur) {
        List<ValeurIndicateur> terminees = valeurIndicateurRepository.findValeursTerminees(
                idIndicateur, LocalDateTime.now());
        if (!terminees.isEmpty()) {
            return terminees.get(0);
        }
        List<ValeurIndicateur> toutes = valeurIndicateurRepository.findToutesValeurs(idIndicateur);
        return toutes.isEmpty() ? null : toutes.get(0);
    }

    private ValeurSuiviDTO versValeurDto(ValeurIndicateur v) {
        return ValeurSuiviDTO.builder()
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

    private AtteinteIndicateurDTO calculerAtteinte(
            Integer idIndicateur,
            String code,
            String designation,
            String typeIndicateur,
            String uniteMesure,
            BigDecimal cible,
            BigDecimal seuilMin,
            BigDecimal seuilMax,
            BigDecimal valeur,
            LocalDateTime periodeDebut,
            LocalDateTime periodeFin) {

        BigDecimal ecart = (valeur != null && cible != null)
                ? valeur.subtract(cible)
                : null;

        String statut;
        if (valeur == null || cible == null || typeIndicateur == null) {
            statut = STATUT_INDETERMINE;
        } else if (TYPE_KPI.equals(typeIndicateur)) {
            statut = valeur.compareTo(cible) >= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else if (TYPE_KRI.equals(typeIndicateur)) {
            statut = valeur.compareTo(cible) <= 0 ? STATUT_ATTEINT : STATUT_NON_ATTEINT;
        } else {
            statut = STATUT_INDETERMINE;
        }

        return AtteinteIndicateurDTO.builder()
                .idIndicateur(idIndicateur)
                .code(code)
                .designation(designation)
                .typeIndicateur(typeIndicateur)
                .uniteMesure(uniteMesure)
                .valeurCible(cible)
                .seuilMin(seuilMin)
                .seuilMax(seuilMax)
                .valeurCourante(valeur)
                .periodeDebut(periodeDebut)
                .periodeFin(periodeFin)
                .ecart(ecart)
                .statut(statut)
                .build();
    }

    private AtteinteIndicateurDTO versAtteinteDto(
            IndicateurDetailRow indicateur,
            ValeurIndicateurRow derniere) {
        return calculerAtteinte(
                indicateur.getId(),
                indicateur.getCode(),
                indicateur.getDesignation(),
                indicateur.getTypeIndicateur(),
                indicateur.getUniteMesure(),
                indicateur.getValeurCible(),
                indicateur.getSeuilMin(),
                indicateur.getSeuilMax(),
                derniere == null ? null : derniere.getValeur(),
                derniere == null ? null : derniere.getPeriodeDebut(),
                derniere == null ? null : derniere.getPeriodeFin());
    }

    private AtteinteIndicateurDTO versAtteinteDto(
            Indicateur indicateur,
            ValeurIndicateur derniere) {
        return calculerAtteinte(
                indicateur.getIdIndicateur(),
                indicateur.getCode(),
                indicateur.getDesignation(),
                indicateur.getTypeIndicateur(),
                indicateur.getUniteMesure(),
                indicateur.getValeurCible(),
                indicateur.getSeuilMin(),
                indicateur.getSeuilMax(),
                derniere == null ? null : derniere.getValeur(),
                derniere == null ? null : derniere.getPeriodeDebut(),
                derniere == null ? null : derniere.getPeriodeFin());
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

    @Transactional(readOnly = true)
    public List<IndicateurEnRetardDTO> listerEnRetard(
            boolean inclureAJour, boolean inclureJamaisSaisi) {

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        Integer servicePerimetre = perimetre.getFiltreService();
        Integer departementPerimetre = perimetre.getFiltreDepartement();

        List<Indicateur> candidats = indicateurRepository.findActifsAvecFrequenceDansPerimetre(
                servicePerimetre, departementPerimetre);

        if (candidats.isEmpty()) {
            return List.of();
        }

        // UNE requete batch pour toutes les valeurs, au lieu d'une par indicateur
        List<Integer> ids = candidats.stream()
                .map(Indicateur::getIdIndicateur)
                .toList();

        Map<Integer, List<ValeurIndicateur>> valeursParIndicateur = valeurIndicateurRepository
                .findToutesValeursPourIndicateurs(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        v -> v.getIndicateur().getIdIndicateur(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<IndicateurEnRetardDTO> resultat = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();

        for (Indicateur indicateur : candidats) {
            List<ValeurIndicateur> valeurs = valeursParIndicateur
                    .getOrDefault(indicateur.getIdIndicateur(), List.of());

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

    // ------------------------------------------------------------------
    // F13 — Calendrier des valeurs sur une année
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CalendrierIndicateurDTO calendrier(Integer idIndicateur, Integer annee) {

        Indicateur indicateur = chargerIndicateurDansPerimetre(idIndicateur);

        int anneeCible = (annee != null) ? annee : LocalDate.now().getYear();
        if (anneeCible < 1900 || anneeCible > 2100) {
            throw new ApiException(
                    "L'année doit être comprise entre 1900 et 2100.",
                    HttpStatus.BAD_REQUEST);
        }

        String freq = indicateur.getFrequenceVerification();
        if (freq == null || freq.isBlank()) {
            freq = "MENSUEL"; // fallback pour ne pas bloquer l'affichage
        }

        LocalDateTime debutAnnee = LocalDate.of(anneeCible, 1, 1).atStartOfDay();
        LocalDateTime finAnnee = LocalDate.of(anneeCible, 12, 31).atTime(23, 59, 59);

        List<ValeurIndicateur> toutes = valeurIndicateurRepository
                .findHistoriqueByIndicateur(idIndicateur);

        List<ValeurIndicateur> valeursAnnee = toutes.stream()
                .filter(v -> !v.getPeriodeFin().isBefore(debutAnnee)
                        && !v.getPeriodeDebut().isAfter(finAnnee))
                .toList();

        List<PeriodeCalendrierDTO> periodes = genererPeriodesCalendrier(
                anneeCible, freq, valeursAnnee, indicateur);

        int saisies = (int) periodes.stream()
                .filter(p -> p.getValeurId() != null)
                .count();

        BigDecimal moyenne = periodes.stream()
                .filter(p -> p.getValeur() != null)
                .map(PeriodeCalendrierDTO::getValeur)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (saisies > 0) {
            moyenne = moyenne.divide(BigDecimal.valueOf(saisies), 2, java.math.RoundingMode.HALF_UP);
        } else {
            moyenne = BigDecimal.ZERO;
        }

        return CalendrierIndicateurDTO.builder()
                .idIndicateur(indicateur.getIdIndicateur())
                .code(indicateur.getCode())
                .designation(indicateur.getDesignation())
                .annee(anneeCible)
                .frequenceVerification(freq)
                .typeIndicateur(indicateur.getTypeIndicateur())
                .uniteMesure(indicateur.getUniteMesure())
                .valeurCible(indicateur.getValeurCible())
                .seuilMin(indicateur.getSeuilMin())
                .seuilMax(indicateur.getSeuilMax())
                .nombrePeriodes(periodes.size())
                .nombreSaisies(saisies)
                .valeurMoyenne(moyenne)
                .periodes(periodes)
                .build();
    }

    private List<PeriodeCalendrierDTO> genererPeriodesCalendrier(
            int annee, String frequence, List<ValeurIndicateur> valeurs, Indicateur indicateur) {

        List<PeriodeCalendrierDTO> resultat = new ArrayList<>();
        String freq = normaliserFrequence(frequence);
        LocalDateTime maintenant = LocalDateTime.now();

        switch (freq) {
            case "MENSUEL" -> {
                for (int m = 1; m <= 12; m++) {
                    LocalDate d = LocalDate.of(annee, m, 1);
                    LocalDate f = d.withDayOfMonth(d.lengthOfMonth());
                    String libelle = d.getMonth().getDisplayName(
                            java.time.format.TextStyle.FULL, Locale.FRENCH);
                    resultat.add(construirePeriode(
                            capitaliser(libelle), d.atStartOfDay(), f.atTime(23, 59, 59),
                            valeurs, indicateur, maintenant));
                }
            }
            case "TRIMESTRIEL" -> {
                for (int t = 1; t <= 4; t++) {
                    LocalDate d = LocalDate.of(annee, (t - 1) * 3 + 1, 1);
                    LocalDate f = d.plusMonths(3).minusDays(1);
                    resultat.add(construirePeriode(
                            "T" + t, d.atStartOfDay(), f.atTime(23, 59, 59),
                            valeurs, indicateur, maintenant));
                }
            }
            case "SEMESTRIEL" -> {
                for (int s = 1; s <= 2; s++) {
                    LocalDate d = LocalDate.of(annee, (s - 1) * 6 + 1, 1);
                    LocalDate f = d.plusMonths(6).minusDays(1);
                    resultat.add(construirePeriode(
                            "S" + s, d.atStartOfDay(), f.atTime(23, 59, 59),
                            valeurs, indicateur, maintenant));
                }
            }
            case "ANNUEL" -> {
                LocalDate d = LocalDate.of(annee, 1, 1);
                LocalDate f = LocalDate.of(annee, 12, 31);
                resultat.add(construirePeriode(
                        String.valueOf(annee), d.atStartOfDay(), f.atTime(23, 59, 59),
                        valeurs, indicateur, maintenant));
            }
            default -> throw new ApiException(
                    "Fréquence de vérification non supportée : « " + frequence + " ».",
                    HttpStatus.BAD_REQUEST);
        }

        return resultat;
    }

    private String normaliserFrequence(String brut) {
        if (brut == null || brut.isBlank()) {
            return "MENSUEL"; // fallback
        }

        String s = brut.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace(" ", "");

        // Retire un eventuel accent (SEMESTRIEL / SEMESTRIEL)
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return switch (s) {
            case "MENSUEL", "MENSUELLE" -> "MENSUEL";
            case "TRIMESTRIEL", "TRIMESTRIELLE" -> "TRIMESTRIEL";
            case "SEMESTRIEL", "SEMESTRIELLE" -> "SEMESTRIEL";
            case "ANNUEL", "ANNUELLE" -> "ANNUEL";
            default -> throw new ApiException(
                    "Fréquence de vérification non supportée : « " + brut + " ». "
                            + "Valeurs acceptées : Mensuel, Trimestriel, Semestriel, Annuel "
                            + "(ou leurs formes féminines).",
                    HttpStatus.BAD_REQUEST);
        };
    }

    private PeriodeCalendrierDTO construirePeriode(
            String libelle, LocalDateTime debut, LocalDateTime fin,
            List<ValeurIndicateur> valeurs, Indicateur indicateur, LocalDateTime maintenant) {

        boolean estEnCours = !maintenant.isBefore(debut) && !maintenant.isAfter(fin);
        boolean estFuture = debut.isAfter(maintenant);
        boolean estPassee = fin.isBefore(maintenant);

        // Recherche d'une valeur dont la periode tombe dans la cellule
        ValeurIndicateur match = valeurs.stream()
                .filter(v -> !v.getPeriodeDebut().isBefore(debut)
                        && !v.getPeriodeDebut().isAfter(fin))
                .findFirst()
                .orElse(null);

        String statut;
        if (match == null) {
            if (estFuture) {
                statut = "NON_SAISI";
            } else if (estEnCours) {
                statut = "EN_COURS";
            } else {
                statut = "MANQUANT";
            }
        } else {
            statut = calculerStatutAtteinte(indicateur, match.getValeur());
        }

        return PeriodeCalendrierDTO.builder()
                .libelle(libelle)
                .debut(debut)
                .fin(fin)
                .valeurId(match == null ? null : match.getIdValeurIndicateur())
                .valeur(match == null ? null : match.getValeur())
                .commentaire(match == null ? null : match.getCommentaire())
                .dateSaisie(match == null ? null : match.getDateSaisie())
                .saisieParLibelle(match == null ? null
                        : match.getUtilisateur().getPrenom() + " " + match.getUtilisateur().getNom())
                .statut(statut)
                .estEnCours(estEnCours)
                .estFuture(estFuture)
                .estPassee(estPassee)
                .build();
    }

    private String calculerStatutAtteinte(Indicateur indicateur, BigDecimal valeur) {
        BigDecimal cible = indicateur.getValeurCible();
        String type = indicateur.getTypeIndicateur();

        if (valeur == null || cible == null || type == null) {
            return "INDETERMINE";
        }
        if ("KPI".equals(type)) {
            return valeur.compareTo(cible) >= 0 ? "ATTEINT" : "NON_ATTEINT";
        }
        if ("KRI".equals(type)) {
            return valeur.compareTo(cible) <= 0 ? "ATTEINT" : "NON_ATTEINT";
        }
        return "INDETERMINE";
    }

    @Transactional(readOnly = true)
    public DashboardIndicateurDTO dashboard() {

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

        List<Indicateur> tous = indicateurRepository
                .findActifsAvecFrequenceDansPerimetre(servicePerimetre, departementPerimetre);

        // Fallback : si aucun indicateur n'a de frequence, prendre tous les actifs
        if (tous.isEmpty()) {
            tous = indicateurRepository.findByActifTrue();
        }

        int atteints = 0;
        int nonAtteints = 0;
        int indetermines = 0;
        int jamaisSaisis = 0;

        List<DashboardIndicateurDTO.IndicateurAvecAtteinteDTO> items = new ArrayList<>();

        for (Indicateur ind : tous) {
            ValeurIndicateur derniere = trouverValeurCourante(ind.getIdIndicateur());

            String statut;
            BigDecimal valeur = null;
            BigDecimal ecart = null;
            BigDecimal cible = ind.getValeurCible();

            if (derniere == null) {
                statut = "JAMAIS_SAISI";
                jamaisSaisis++;
            } else {
                valeur = derniere.getValeur();
                if (cible != null) {
                    ecart = valeur.subtract(cible);
                }
                statut = calculerStatutAtteinte(ind, valeur);

                if ("ATTEINT".equals(statut))
                    atteints++;
                else if ("NON_ATTEINT".equals(statut))
                    nonAtteints++;
                else
                    indetermines++;
            }

            items.add(DashboardIndicateurDTO.IndicateurAvecAtteinteDTO.builder()
                    .id(ind.getIdIndicateur())
                    .code(ind.getCode())
                    .designation(ind.getDesignation())
                    .typeIndicateur(ind.getTypeIndicateur())
                    .uniteMesure(ind.getUniteMesure())
                    .valeurCible(cible)
                    .valeurCourante(valeur)
                    .ecart(ecart)
                    .statut(statut)
                    .frequenceVerification(ind.getFrequenceVerification())
                    .build());
        }

        // Tri : non atteints d'abord, puis indéterminés, puis jamais saisis, puis
        // atteints
        items.sort(Comparator.comparing(
                DashboardIndicateurDTO.IndicateurAvecAtteinteDTO::getStatut,
                Comparator.comparingInt(this::prioriteStatut)));

        // En retard (reutilise la methode existante)
        List<IndicateurEnRetardDTO> enRetard = listerEnRetard(false, true);

        return DashboardIndicateurDTO.builder()
                .resume(DashboardIndicateurDTO.ResumeDTO.builder()
                        .total(tous.size())
                        .atteints(atteints)
                        .nonAtteints(nonAtteints)
                        .indetermines(indetermines)
                        .enRetard(enRetard.size())
                        .jamaisSaisis(jamaisSaisis)
                        .build())
                .indicateurs(items)
                .enRetard(enRetard)
                .build();
    }

    /**
     * Priorite d'affichage : non atteints et manquants d'abord.
     */
    private int prioriteStatut(String statut) {
        return switch (statut) {
            case "NON_ATTEINT" -> 0;
            case "INDETERMINE" -> 1;
            case "JAMAIS_SAISI" -> 2;
            case "ATTEINT" -> 3;
            default -> 4;
        };
    }

    private String capitaliser(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}