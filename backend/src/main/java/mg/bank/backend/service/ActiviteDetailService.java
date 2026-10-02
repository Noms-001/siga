package mg.bank.backend.service;

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
import mg.bank.backend.dto.ActiviteDetailDTO;
import mg.bank.backend.dto.AffectationDetailDTO;
import mg.bank.backend.dto.AvancementDetailDTO;
import mg.bank.backend.dto.EtapeValidationDTO;
import mg.bank.backend.dto.FichierDetailDTO;
import mg.bank.backend.dto.FichierTelechargeable;
import mg.bank.backend.dto.HistoriqueActiviteDTO;
import mg.bank.backend.dto.IndicateurDetailDTO;
import mg.bank.backend.dto.LivrableDetailDTO;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.dto.ResultatIntermediaireDTO;
import mg.bank.backend.dto.SousActiviteDetailDTO;
import mg.bank.backend.dto.UtilisateurResumeDTO;
import mg.bank.backend.dto.ValidationActiviteDTO;
import mg.bank.backend.dto.ValeurIndicateurDTO;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.projection.ActiviteDetailRow;
import mg.bank.backend.repository.projection.AffectationDetailRow;
import mg.bank.backend.repository.projection.AvancementDetailRow;
import mg.bank.backend.repository.projection.FichierDetailRow;
import mg.bank.backend.repository.projection.FichierTelechargementRow;
import mg.bank.backend.repository.projection.HistoriqueActiviteRow;
import mg.bank.backend.repository.projection.IndicateurDetailRow;
import mg.bank.backend.repository.projection.LivrableDetailRow;
import mg.bank.backend.repository.projection.ResultatIntermediaireRow;
import mg.bank.backend.repository.projection.SousActiviteDetailRow;
import mg.bank.backend.repository.projection.ValidationActiviteRow;
import mg.bank.backend.repository.projection.ValeurIndicateurRow;

/**
 * Assemblage du detail d une activite.
 *
 * SEPARATION DES RESPONSABILITES
 * Ce service ne fait pas que lire : il regroupe. Le detail combine onze tables,
 * dont cinq directement rattachees a l activite et trois qui passent par la
 * sous-activite. Aucune de ces tables n a d entite JPA dans le projet, et
 * aucune ne justifie d en creer une : elles ne sont consultees que par cette
 * lecture, jamais modifiees, et quelques-unes -- affectation_sous_activite,
 * activite_indicateur -- ne sont que des tables de jointure. Des projections
 * et des requetes natives suffisent, et evitent d exposer un modele d ecriture
 * qui n a pas d usage.
 *
 * NOMBRE DE REQUETES
 * Le detail execute un nombre FIXE de requetes, independant du contenu de
 * l activite : une pour l activite, puis une par type de donnee enfant, jamais
 * une par sous-activite ni par livrable. Les sous-activites, affectations,
 * avancements, livrables, fichiers, validations, indicateurs, mesures,
 * resultats et historique sont donc lus en dix requetes, quel que soit le
 * nombre de sous-activites. Le regroupement se fait ensuite en memoire, par
 * identifiant de parent.
 *
 * Cette propriete n est pas automatique : elle tient au fait que chaque
 * requete enfant est un JOIN depuis l activite, et non une requete par
 * identifiant parent. Revenir a une boucle appelant le repository pour chaque
 * sous-activite la casserait silencieusement, avec un impact qui ne se voit
 * qu a l echelle.
 *
 * PERIMETRE
 * Le perimetre est delegue a ActiviteService, qui reste la seule source de
 * verite : il n est pas recalcule ici, pour que les deux lectures ne puissent
 * pas diverger sur ce qui autorise un acces. L identite de l appelant, elle,
 * n entre pas dans la base : elle est lue dans le contexte de securite, jamais
 * transmise par le client.
 */
@Service
@RequiredArgsConstructor
public class ActiviteDetailService {

    private final ActiviteRepository activiteRepository;

    /**
     * Utilise uniquement pour son perimetre.
     *
     * Injecter un service dans un autre service est inhabituel ici, mais
     * isole le detail sans dupliquer la reconstruction du perimetre, qui doit
     * rester identique a celle de la liste pour que les deux endpoint
     * racontent la meme histoire.
     */
    private final ActiviteService activiteService;

    /**
     * Detail d une activite, avec ses sous-activites, livrables, fichiers,
     * validations, indicateurs, resultats et historique.
     */
    @Transactional(readOnly = true)
    public ActiviteDetailDTO detail(Integer id) {

        if (id == null || id < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        ActiviteDetailRow row = activiteRepository.findActiviteDetail(
                        id,
                        perimetre.estRattacheService() ? perimetre.getIdService() : null,
                        perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null,
                        codesTerminaux())
                .orElseThrow(() -> refusOuAbsence(id));

        return assembler(row);
    }

    /**
     * Deux questions a distinguer, une seule est resolue ici.
     *
     * L activite peut ne pas exister, auquel cas 404 : c est une erreur de
     * saisie ou un lien obsolete. Elle peut aussi exister et appartenir a un
     * autre perimetre, auquel cas 403 : la requete est comprise et refusee.
     *
     * Les confondre en renvoyant 404 dans les deux cas donnerait un message
     * trompeur -- une activite reellement interdite paraitrait inexistante --
     * et surtout interdirait a l appelant de distinguer une faute de frappe
     * d un acces refuse. Le coup de la verification supplementaire n est paye
     * que sur ce chemin, qui est celui de l erreur.
     *
     * Le message du 403 ne nomme ni l activite ni son service : il confirme
     * l acces refuse, pas la position de la donnee.
     *
     * La regle elle-meme est portee par ActiviteService, comme le perimetre :
     * une ecriture doit pouvoir repondre 403 sur la meme activite, sans que
     * les deux services puissent diverger sur ce qui autorise un acces.
     */
    private ApiException refusOuAbsence(Integer id) {
        return activiteService.refusOuAbsence(id);
    }

    /**
     * Detail d une sous-activite, avec son historique d avancement, ses
     * affectations et ses livrables avec fichiers.
     *
     * Trois ressources sont en jeu et le 404 n a pas la meme portee partout :
     * une sous-activite inconnue, ou rattachee a une autre activite que celle
     * demandee, n existe pas dans le chemin demande : 404. Une sous-activite
     * d une activite existante mais hors perimetre existe : 403. La
     * distinction est celle du detail d activite, etendue a deux
     * identifiants.
     */
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

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        SousActiviteDetailRow row = activiteRepository.findSousActivite(
                        idActivite,
                        idSousActivite,
                        perimetre.estRattacheService() ? perimetre.getIdService() : null,
                        perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null)
                .orElseThrow(() -> refusOuAbsenceSousActivite(
                        idActivite, idSousActivite));

        return assemblerSousActivite(row);
    }

    /**
     * Meme distinction que pour l activite, portee a deux identifiants.
     *
     * Une sous-activite inconnue et une sous-activite d une autre activite
     * renvoient toutes deux 404 : dans les deux cas la ressource n existe pas
     * dans le chemin demande, et nommer l activite ou la sous-activite
     * n aiderait pas. Reste le 403 : la sous-activite existe, son activite
     * aussi, mais elle sort du perimetre. Ces comptages ne sont payes que sur
     * le chemin de l erreur, jamais en fonctionnement nominal.
     *
     * PUBLIC, ET POUR LA MEME RAISON QUE getPerimetre ET refusOuAbsence
     *
     * Une ecriture qui porte sur une sous-activite -- y ajouter un livrable --
     * doit rendre exactement le meme 403 et le meme 404. Dupliquer ces trois
     * comptages ailleurs autoriserait les deux reponses a diverger : un jour
     * l une nommerait la sous-activite sur un 403 que l autre refuse de
     * nommer. C est aussi ce qui permet au service d ecriture de s appuyer sur
     * l echec de findSousActivite, qui signifie deja "inexistante ou hors
     * perimetre".
     */
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

    /**
     * Fichiers d un livrable, decrits comme le detail les decrit.
     *
     * PUBLIC POUR LE SERVICE D ECRITURE, QUI AJOUTE DES FICHIERS
     *
     * Apres un ajout, la reponse doit decrire le livrable tel que le detail le
     * decrirait -- meme tri, meme auteur, meme forme. Reconstruire cette liste
     * dans le service d ecriture depuis les lignes qu il vient d ecrire
     * donnerait deux versions de la meme description, qui divergeraient a la
     * premiere evolution de l une des deux. Passer par ici garantit qu il n y
     * en a qu une.
     *
     * Le perimetre n est pas revalide ici : l appelant a deja passe par
     * findSousActivite, qui l applique, et il n appelle cette methode que pour
     * un livrable dont il a verifie l appartenance a cette sous-activite.
     */
    public List<FichierDetailDTO> fichiers(Integer idLivrable) {

        return activiteRepository.findFichiersLivrable(idLivrable).stream()
                .map(this::fichier)
                .toList();
    }

    /**
     * Assemblage du detail propre d une sous-activite, par reutilisation de
     * celui du detail d activite : meme DTO, meme definition de l avancement
     * courant (premiere ligne du groupe trie), memes regles de regroupement.
     *
     * Le nombre de requetes est fixe : cinq, quel que soit le nombre de
     * releves, de responsables ou de livrables. Aucune boucle n appelle le
     * repository par identifiant enfant.
     */
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

    /**
     * Lecture du contenu d un fichier, sous les memes regles que le detail
     * de sous-activite : fichier d une sous-activite de l activite demandee,
     * le tout dans le perimetre de l appelant.
     *
     * L endpoint renvoie le CONTENU, jamais le chemin de stockage : le chemin
     * ne sert qu a retrouver le depot sur le disque du serveur. Sa fuite en
     * reponse laisserait voir l arborescence du serveur pour un simple
     * telechargement.
     *
     * Un fichier reference en base mais absent du disque est un 404 : le
     * fichier n existe plus, meme si son enregistrement date.
     */
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

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        FichierTelechargementRow ligne = activiteRepository
                .findFichierTelechargement(
                        idActivite, idSousActivite, idFichier,
                        perimetre.estRattacheService() ? perimetre.getIdService() : null,
                        perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null)
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

    /**
     * Meme departage 404/403 que pour la sous-activite, descendu jusqu au
     * fichier : un fichier inconnu, ou d une autre sous-activite que celle du
     * chemin, ou d une sous-activite d une autre activite, renvoie 404. Seul
     * reste le 403, celui d une activite existante hors perimetre. Ces
     * comptages ne sont payes que sur le chemin de l erreur.
     */
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

    /**
     * Statuts terminaux, memes codes que la liste.
     *
     * Appeles depuis deux services mais a partir de la meme source, donc sans
     * possibilite de desynchronisation. Les lister dans chaque service aurait
     * autorise le cas ou l un evolve et pas l autre, et le detail afficherait
     * alors un enRetard different de celui des cards.
     */
    private String codesTerminaux() {
        return String.join(",", StatutActiviteEnum.codesTerminaux());
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
                .validations(activiteRepository.findValidations(id).stream()
                        .map(this::validation)
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

    /**
     * Regroupe des lignes par identifiant de parent, en conservant l ordre de
     * lecture a l interieur de chaque groupe.
     *
     * LinkedHashMap : l encounter du premier parent fixe sa position, et
     * l encounter de ses lignes fixe leur rang. Le regroupement ne melange pas
     * l ordre, il le conserve, et c est ce qui permet de designer l avancement
     * courant ou la valeur courante par la premiere ligne du groupe plutot
     * que par une recherche.
     */
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

    private ValidationActiviteDTO validation(ValidationActiviteRow row) {
        return ValidationActiviteDTO.builder()
                .id(row.getId())
                .decision(row.getDecision())
                .commentaire(row.getCommentaire())
                .dateDemande(row.getDateDemande())
                .dateDecision(row.getDateDecision())
                .etape(EtapeValidationDTO.builder()
                        .id(row.getEtapeId())
                        .designation(row.getEtapeDesignation())
                        .description(row.getEtapeDescription())
                        .niveau(row.getEtapeNiveau())
                        .obligatoire(row.getEtapeObligatoire())
                        .procedureLibelle(row.getEtapeProcedureLibelle())
                        .build())
                .demandeur(utilisateur(
                        row.getDemandeurId(),
                        row.getDemandeurNom(),
                        row.getDemandeurPrenom()))
                .decideur(utilisateur(
                        row.getDecideurId(),
                        row.getDecideurNom(),
                        row.getDecideurPrenom()))
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

    /**
     * Reference nulle si l identifiant est absent.
     *
     * Le statut courant passe par ici : une activite sans historique n a pas
     * de statut, et la liste renvoie alors un statut null plutot qu une
     * reference a moitie vide. Le detail fait de meme.
     */
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
