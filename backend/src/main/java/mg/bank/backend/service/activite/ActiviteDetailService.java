package mg.bank.backend.service.activite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteDetailDTO;
import mg.bank.backend.dto.activite.AvancementDetailDTO;
import mg.bank.backend.dto.activite.FichierDetailDTO;
import mg.bank.backend.dto.activite.FichierTelechargeable;
import mg.bank.backend.dto.activite.HistoriqueActiviteDTO;
import mg.bank.backend.dto.activite.LivrableDetailDTO;
import mg.bank.backend.dto.activite.ResultatIntermediaireDTO;
import mg.bank.backend.dto.activite.SousActiviteDetailDTO;
import mg.bank.backend.dto.affectation.AffectationDetailDTO;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.common.UtilisateurResumeDTO;
import mg.bank.backend.dto.indicateur.IndicateurDetailDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurDTO;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.projection.activite.ActiviteDetailRow;
import mg.bank.backend.repository.projection.activite.AvancementDetailRow;
import mg.bank.backend.repository.projection.activite.FichierDetailRow;
import mg.bank.backend.repository.projection.activite.FichierTelechargementRow;
import mg.bank.backend.repository.projection.activite.HistoriqueActiviteRow;
import mg.bank.backend.repository.projection.activite.LivrableDetailRow;
import mg.bank.backend.repository.projection.activite.ResultatIntermediaireRow;
import mg.bank.backend.repository.projection.activite.SousActiviteDetailRow;
import mg.bank.backend.repository.projection.affectation.AffectationDetailRow;
import mg.bank.backend.repository.projection.indicateur.IndicateurDetailRow;
import mg.bank.backend.repository.projection.indicateur.ValeurIndicateurRow;
import mg.bank.backend.service.utilisateur.CurrentUserService;


@Service
@RequiredArgsConstructor
public class ActiviteDetailService {

    private final ActiviteRepository activiteRepository;

    private final ActiviteService activiteService;

    private final CurrentUserService currentUserService;

    /**
     * Detail d une activite, avec ses sous-activites, livrables, fichiers,
     * indicateurs, resultats et historique.
     */
    @Transactional(readOnly = true)
    public ActiviteDetailDTO detail(Integer id) {

        if (id == null || id < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        ActiviteDetailRow row = activiteRepository.findActiviteDetail(
                        id,
                        perimetre.getFiltreService(),
                        perimetre.getFiltreDepartement(),
                        codesTerminaux())
                .orElseThrow(() -> refusOuAbsence(id));

        return assembler(row);
    }

    private ApiException refusOuAbsence(Integer id) {
        return activiteService.refusOuAbsence(id);
    }

    @Transactional(readOnly = true)
    public SousActiviteDetailDTO detailSousActivite(
            Integer idActivite,
            Integer idSousActivite) {

        if (idActivite == null || idActivite < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        if (idSousActivite == null || idSousActivite < 1) {
            throw new ApiException(
                    "L identifiant de la sous-activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        SousActiviteDetailRow row = activiteRepository.findSousActivite(
                        idActivite,
                        idSousActivite,
                        perimetre.getFiltreService(),
                        perimetre.getFiltreDepartement())
                .orElseThrow(() -> refusOuAbsenceSousActivite(
                        idActivite, idSousActivite));

        return assemblerSousActivite(row);
    }

    public ApiException refusOuAbsenceSousActivite(
            Integer idActivite,
            Integer idSousActivite) {

        if (activiteRepository.compterSousActivite(idSousActivite) == 0) {
            return new ApiException(
                    "Sous-activite introuvable", HttpStatus.NOT_FOUND);
        }

        if (activiteRepository.compterSousActiviteDeActivite(
                idActivite, idSousActivite) == 0) {
            return new ApiException(
                    "Sous-activite introuvable", HttpStatus.NOT_FOUND);
        }

        return new ApiException(
                "Vous n etes pas autorise a consulter cette sous-activite",
                HttpStatus.FORBIDDEN);
    }

    public List<FichierDetailDTO> fichiers(Integer idLivrable) {

        return activiteRepository.findFichiersLivrable(idLivrable).stream()
                .map(this::fichier)
                .toList();
    }

    private SousActiviteDetailDTO assemblerSousActivite(SousActiviteDetailRow row) {

        Integer id = row.getId();

        return sousActivite(
                row,
                activiteRepository.findAvancementsSousActivite(id),
                activiteRepository.findAffectationsSousActivite(id),
                activiteRepository.findLivrablesSousActivite(id),
                parCle(activiteRepository.findFichiersSousActivite(id),
                        FichierDetailRow::getIdLivrable));
    }

    @Transactional(readOnly = true)
    public FichierTelechargeable telechargerFichier(
            Integer idActivite,
            Integer idSousActivite,
            Integer idFichier) {

        if (idFichier == null || idFichier < 1) {
            throw new ApiException(
                    "L identifiant du fichier doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        if (idSousActivite == null || idSousActivite < 1) {
            throw new ApiException(
                    "L identifiant de la sous-activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        if (idActivite == null || idActivite < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        FichierTelechargementRow ligne = activiteRepository
                .findFichierTelechargement(
                        idActivite, idSousActivite, idFichier,
                        perimetre.getFiltreService(),
                        perimetre.getFiltreDepartement())
                .orElseThrow(() -> refusOuAbsenceFichier(
                        idActivite, idSousActivite, idFichier));

        Path chemin = Path.of(ligne.getCheminFichier()).normalize();

        if (!Files.isRegularFile(chemin)) {
            throw new ApiException(
                    "Le fichier n est plus disponible sur le serveur",
                    HttpStatus.NOT_FOUND);
        }

        try {
            return new FichierTelechargeable(
                    ligne.getNomOriginal(),
                    ligne.getTypeMime(),
                    Files.readAllBytes(chemin));
        } catch (IOException e) {
            throw new ApiException(
                    "Lecture du fichier impossible",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ApiException refusOuAbsenceFichier(
            Integer idActivite,
            Integer idSousActivite,
            Integer idFichier) {

        if (activiteRepository.compterFichier(idFichier) == 0) {
            return new ApiException("Fichier introuvable", HttpStatus.NOT_FOUND);
        }

        if (activiteRepository.compterFichierDeSousActivite(
                idSousActivite, idFichier) == 0) {
            return new ApiException("Fichier introuvable", HttpStatus.NOT_FOUND);
        }

        if (activiteRepository.compterSousActiviteDeActivite(
                idActivite, idSousActivite) == 0) {
            return new ApiException("Fichier introuvable", HttpStatus.NOT_FOUND);
        }

        return new ApiException(
                "Vous n etes pas autorise a consulter ce fichier",
                HttpStatus.FORBIDDEN);
    }

    private String codesTerminaux() {
        return StatutActiviteEnum.codesTerminauxSql();
    }

    // ------------------------------------------------------------------
    // Assemblage
    // ------------------------------------------------------------------

    private ActiviteDetailDTO assembler(ActiviteDetailRow row) {

        Integer id = row.getId();

        List<SousActiviteDetailRow> sousActivites =
                activiteRepository.findSousActivites(id);

        // Regroupement par identifiant de parent. Chaque groupe est lu dans
        // l ordre impose par sa requete, et cet ordre porte du sens : la
        // premiere ligne est toujours la plus recente.
        Map<Integer, List<AvancementDetailRow>> avancements =
                parCle(activiteRepository.findAvancements(id),
                        AvancementDetailRow::getIdSousActivite);

        Map<Integer, List<AffectationDetailRow>> affectations =
                parCle(activiteRepository.findAffectations(id),
                        AffectationDetailRow::getIdSousActivite);

        Map<Integer, List<LivrableDetailRow>> livrables =
                parCle(activiteRepository.findLivrables(id),
                        LivrableDetailRow::getIdSousActivite);

        Map<Integer, List<FichierDetailRow>> fichiers =
                parCle(activiteRepository.findFichiers(id),
                        FichierDetailRow::getIdLivrable);

        Map<Integer, List<ValeurIndicateurRow>> valeurs =
                parCle(activiteRepository.findValeursIndicateurs(id),
                        ValeurIndicateurRow::getIdIndicateur);

        return ActiviteDetailDTO.builder()
                .id(id)
                .code(row.getCode())
                .reference(row.getReference())
                .designation(row.getDesignation())
                .dateDebutPrevue(row.getDateDebutPrevue())
                .dateFinPrevue(row.getDateFinPrevue())
                .dateDebutReelle(row.getDateDebutReelle())
                .dateFinReelle(row.getDateFinReelle())
                .dateCreation(row.getDateCreation())
                .pta(row.getOsId() != null)
                .objectifSpecifique(reference(
                        row.getOsId(), row.getOsCode(),
                        row.getOsDesignation(), row.getOsAnnee()))
                .service(reference(
                        row.getServiceId(), null, row.getServiceLibelle(), null))
                .typeActivite(reference(
                        row.getTypeActiviteId(), null,
                        row.getTypeActiviteLibelle(), null))
                .site(reference(row.getSiteId(), null, row.getSiteLibelle(), null))
                .priorite(reference(
                        row.getPrioriteId(), row.getPrioriteCode(),
                        row.getPrioriteLibelle(), null))
                .statut(reference(
                        row.getStatutId(), row.getStatutCode(),
                        row.getStatutLibelle(), null))
                .avancement(row.getAvancement() == null ? 0d : row.getAvancement())
                .enRetard(Boolean.TRUE.equals(row.getEnRetard()))
                .sousActivites(sousActivites.stream()
                        .map(sa -> sousActivite(sa,
                                avancements.getOrDefault(sa.getId(), List.of()),
                                affectations.getOrDefault(sa.getId(), List.of()),
                                livrables.getOrDefault(sa.getId(), List.of()),
                                fichiers))
                        .toList())
                .indicateurs(activiteRepository.findIndicateurs(id).stream()
                        .map(i -> indicateur(i, valeurs.getOrDefault(i.getId(), List.of())))
                        .toList())
                .resultatsIntermediaires(activiteRepository
                        .findResultatsIntermediaires(id).stream()
                        .map(this::resultatIntermediaire)
                        .toList())
                .historique(historique(activiteRepository.findHistorique(id)))
                .build();
    }

    private <T> Map<Integer, List<T>> parCle(
            List<T> lignes,
            Function<T, Integer> cle) {

        if (lignes == null) {
            return Map.of();
        }

        return lignes.stream().collect(Collectors.groupingBy(
                cle, LinkedHashMap::new, Collectors.toList()));
    }

    private SousActiviteDetailDTO sousActivite(
            SousActiviteDetailRow row,
            List<AvancementDetailRow> avancements,
            List<AffectationDetailRow> affectations,
            List<LivrableDetailRow> livrables,
            Map<Integer, List<FichierDetailRow>> fichiers) {

        // La requete trie chaque groupe du plus recent au plus ancien : la
        // premiere ligne est le releve courant, les suivantes le suivent.
        List<AvancementDetailDTO> suivi = new ArrayList<>(avancements.size());

        for (int i = 0; i < avancements.size(); i++) {
            AvancementDetailRow avancement = avancements.get(i);
            suivi.add(AvancementDetailDTO.builder()
                    .id(avancement.getId())
                    .valeurPourcentage(avancement.getValeurPourcentage())
                    .commentaire(avancement.getCommentaire())
                    .dateChangement(avancement.getDateChangement())
                    .statut(reference(
                            avancement.getStatutId(),
                            avancement.getStatutCode(),
                            avancement.getStatutLibelle(),
                            null))
                    .utilisateur(utilisateur(
                            avancement.getUtilisateurId(),
                            avancement.getUtilisateurNom(),
                            avancement.getUtilisateurPrenom()))
                    .etatActuel(i == 0)
                    .build());
        }

        return SousActiviteDetailDTO.builder()
                .id(row.getId())
                .code(row.getCode())
                .designation(row.getDesignation())
                .dateDebutPrevue(row.getDateDebutPrevue())
                .dateFinPrevue(row.getDateFinPrevue())
                .dateDebutReelle(row.getDateDebutReelle())
                .dateFinReelle(row.getDateFinReelle())
                // Nullable quand l'activite n'a pas d'historique : l'absence
                // est transmise telle quelle, pour que le formulaire refuse
                // l'ecriture plutot que de la laisser passer.
                .statutActivite(row.getStatutActivite())
                .avancementCourant(suivi.isEmpty() ? null : suivi.getFirst())
                .historiqueAvancement(suivi)
                .affectations(affectations.stream()
                        .map(this::affectation)
                        .toList())
                .livrables(livrables.stream()
                        .map(l -> livrable(l, fichiers.getOrDefault(l.getId(), List.of())))
                        .toList())
                .build();
    }

    private LivrableDetailDTO livrable(
            LivrableDetailRow row,
            List<FichierDetailRow> fichiers) {

        return LivrableDetailDTO.builder()
                .id(row.getId())
                .designation(row.getDesignation())
                .description(row.getDescription())
                .fichiers(fichiers.stream()
                        .map(this::fichier)
                        .toList())
                .build();
    }

    private FichierDetailDTO fichier(FichierDetailRow row) {
        return FichierDetailDTO.builder()
                .id(row.getId())
                .nomFichier(row.getNomFichier())
                .nomOriginal(row.getNomOriginal())
                .extension(row.getExtension())
                .typeMime(row.getTypeMime())
                .taille(row.getTaille())
                .version(row.getVersion())
                .dateDepot(row.getDateDepot())
                .utilisateur(utilisateur(
                        row.getUtilisateurId(),
                        row.getUtilisateurNom(),
                        row.getUtilisateurPrenom()))
                .build();
    }

    private AffectationDetailDTO affectation(AffectationDetailRow row) {
        return AffectationDetailDTO.builder()
                .id(row.getId())
                .dateAffectation(row.getDateAffectation())
                .dateDesaffectation(row.getDateDesaffectation())
                .role(reference(
                        row.getRoleId(), row.getRoleCode(), row.getRoleLibelle(), null))
                .utilisateur(utilisateur(
                        row.getUtilisateurId(),
                        row.getUtilisateurNom(),
                        row.getUtilisateurPrenom()))
                .build();
    }

    private IndicateurDetailDTO indicateur(
            IndicateurDetailRow row,
            List<ValeurIndicateurRow> valeurs) {

        List<ValeurIndicateurDTO> mesures = new ArrayList<>(valeurs.size());

        for (int i = 0; i < valeurs.size(); i++) {
            ValeurIndicateurRow valeur = valeurs.get(i);
            mesures.add(ValeurIndicateurDTO.builder()
                    .id(valeur.getId())
                    .valeur(valeur.getValeur())
                    .periodeDebut(valeur.getPeriodeDebut())
                    .periodeFin(valeur.getPeriodeFin())
                    .commentaire(valeur.getCommentaire())
                    .dateSaisie(valeur.getDateSaisie())
                    .utilisateur(utilisateur(
                            valeur.getUtilisateurId(),
                            valeur.getUtilisateurNom(),
                            valeur.getUtilisateurPrenom()))
                    .valeurCourante(i == 0)
                    .build());
        }

        return IndicateurDetailDTO.builder()
                .id(row.getId())
                .code(row.getCode())
                .codeHopex(row.getCodeHopex())
                .indicateurHopex(row.getIndicateurHopex())
                .designation(row.getDesignation())
                .typeIndicateur(row.getTypeIndicateur())
                .uniteMesure(row.getUniteMesure())
                .frequenceVerification(row.getFrequenceVerification())
                .frequenceAggregation(row.getFrequenceAggregation())
                .definition(row.getDefinition())
                .methodeDetermination(row.getMethodeDetermination())
                .objectif(row.getObjectif())
                .valeurCible(row.getValeurCible())
                .seuilMin(row.getSeuilMin())
                .seuilMax(row.getSeuilMax())
                .actif(row.getActif())
                .valeurCourante(mesures.isEmpty() ? null : mesures.getFirst())
                .valeurs(mesures)
                .build();
    }

    private ResultatIntermediaireDTO resultatIntermediaire(ResultatIntermediaireRow row) {
        return ResultatIntermediaireDTO.builder()
                .id(row.getId())
                .designation(row.getDesignation())
                .build();
    }

    /**
     * Historique du plus recent au plus ancien, la premiere ligne portant le
     * statut courant.
     *
     * C est le meme statut que celui lu par la requete mere, obtenu par le
     * meme critere de selection. Les deux pourraient diverger si les requetes
     * changeaient separement : la coherence tient a ce qu elles trient toutes
     * deux par (date_changement decroissant, identifiant decroissant), donc a
     * ce que l ordre de lecture designe la meme ligne.
     */
    private List<HistoriqueActiviteDTO> historique(List<HistoriqueActiviteRow> lignes) {

        if (lignes == null) {
            return List.of();
        }

        List<HistoriqueActiviteDTO> entrees = new ArrayList<>(lignes.size());

        for (int i = 0; i < lignes.size(); i++) {
            HistoriqueActiviteRow row = lignes.get(i);
            entrees.add(HistoriqueActiviteDTO.builder()
                    .id(row.getId())
                    .statut(reference(
                            row.getStatutId(),
                            row.getStatutCode(),
                            row.getStatutLibelle(),
                            null))
                    .dateChangement(row.getDateChangement())
                    .commentaire(row.getCommentaire())
                    .utilisateur(utilisateur(
                            row.getUtilisateurId(),
                            row.getUtilisateurNom(),
                            row.getUtilisateurPrenom()))
                    .etatActuel(i == 0)
                    .build());
        }

        return entrees;
    }

    // ------------------------------------------------------------------
    // Aides
    // ------------------------------------------------------------------
    private ReferenceDTO reference(
            Integer id,
            String code,
            String libelle,
            Integer annee) {

        if (id == null) {
            return null;
        }

        return ReferenceDTO.builder()
                .id(id)
                .code(code)
                .libelle(libelle)
                .annee(annee)
                .build();
    }

    private UtilisateurResumeDTO utilisateur(Integer id, String nom, String prenom) {
        return UtilisateurResumeDTO.videSi(id, nom, prenom);
    }

}
