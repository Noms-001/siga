package mg.bank.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteEcritureRequest;
import mg.bank.backend.dto.ActiviteEcritureResponse;
import mg.bank.backend.dto.ActiviteFormulaireDTO;
import mg.bank.backend.dto.LivrableEcritureRequest;
import mg.bank.backend.dto.LivrableFormulaireDTO;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.ResultatIntermediaireEcritureRequest;
import mg.bank.backend.dto.ResultatIntermediaireFormulaireDTO;
import mg.bank.backend.dto.SousActiviteEcritureRequest;
import mg.bank.backend.dto.SousActiviteFormulaireDTO;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.Livrable;
import mg.bank.backend.model.ObjectifSpecifique;
import mg.bank.backend.model.Priorite;
import mg.bank.backend.model.ResultatIntermediaire;
import mg.bank.backend.model.Site;
import mg.bank.backend.model.SousActivite;
import mg.bank.backend.model.TypeActivite;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.LivrableRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.ResultatIntermediaireRepository;
import mg.bank.backend.repository.ServiceRepository;
import mg.bank.backend.repository.SiteRepository;
import mg.bank.backend.repository.SousActiviteRepository;
import mg.bank.backend.repository.TypeActiviteRepository;
import mg.bank.backend.repository.projection.StatutCourantRow;

/**
 * Ecriture d une activite et de ses sous-activites.
 *
 * CE QUE CE SERVICE FAIT, ET CE QU IL REFUSE
 *
 * Creer une activite, toujours en brouillon, et modifier une activite qui n est
 * pas sortie du brouillon. Aucune suppression d activite, aucun changement de
 * statut, aucun avancement : ces operations relevent du suivi et de la
 * validation, pas de la redaction.
 *
 * LES PERMISSIONS NE SONT PAS VERIFIEES
 *
 * Les tables permission et poste_permission existent en base mais aucun
 * chemin de code ne les lit. Ce service ne s'en sert pas : le seul controle
 * reel est le perimetre, service ou departement, applique plus bas.
 *
 * LE STATUT N EST JAMAIS CHOISI PAR LE CLIENT
 *
 * activite n a pas de colonne statut : le statut d une activite neuve est une
 * premiere ligne d historique a BROUILLON, posee ici, et rien d autre ne peut
 * la faire. Une activite modifiee ne change pas d etat, la modification est une
 * redaction et non une transition.
 *
 * LE VERROU EST PRIS AVANT LA RELECTURE DU STATUT
 *
 * Comme pour la soumission : verifier le statut puis ecrire sans verrou
 * ouvrirait une fenetre ou deux onglets modifient le meme brouillon en
 * simultaneite.
 *
 * LES SOUS-ACTIVITES SONT SYNCHRONISEES, PAS RECREEES
 *
 * Une modification ne fait pas "DELETE puis INSERT" de toutes les
 * sous-activites : cela reinitialiserait leurs identifiants, casserait les
 * affectations, livrables et avancements qui y sont rattaches, et ferait
 * disparaitre un brouillon sans que l utilisateur l ait demande. Chaque ligne
 * est donc ajoutee, modifiee ou supprimee selon ce que le formulaire renvoie
 * reellement, et la suppression est refusee tant que la ligne porte une
 * dependance.
 */
@Service
@RequiredArgsConstructor
public class ActiviteEcritureService {

    private final ActiviteRepository activiteRepository;

    private final SousActiviteRepository sousActiviteRepository;

    private final ResultatIntermediaireRepository resultatIntermediaireRepository;

    private final LivrableRepository livrableRepository;

    private final PrioriteRepository prioriteRepository;

    private final TypeActiviteRepository typeActiviteRepository;

    private final SiteRepository siteRepository;

    private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;

    private final ServiceRepository serviceRepository;

    private final ActiviteService activiteService;

    @Transactional(readOnly = true)
    public ActiviteFormulaireDTO formulaire(Integer idActivite) {

        validerId(idActivite);

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        Activite activite = activiteRepository.findById(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        StatutCourantRow statut = statutCourant(idActivite, perimetre)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        List<SousActiviteFormulaireDTO> sousActivites = sousActiviteRepository
                .findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
                        activite.getIdActivite())
                .stream()
                .map(this::versFormulaireSousActivite)
                .toList();

        List<ResultatIntermediaireFormulaireDTO> resultatsIntermediaires
                = resultatIntermediaireRepository
                        .findByActiviteIdActiviteOrderByIdResultatIntermediaireAsc(
                                activite.getIdActivite())
                        .stream()
                        .map(this::versFormulaireResultatIntermediaire)
                        .toList();

        return ActiviteFormulaireDTO.builder()
                .idActivite(activite.getIdActivite())
                .code(activite.getCode())
                .reference(activite.getReference())
                .designation(activite.getDesignation())
                .dateDebutPrevue(activite.getDateDebutPrevue())
                .dateFinPrevue(activite.getDateFinPrevue())
                .idObjectifSpecifique(
                        activite.getObjectifSpecifique() != null
                        ? activite.getObjectifSpecifique()
                                .getIdObjectifSpecifique()
                        : null)
                .objectifCode(
                        activite.getObjectifSpecifique() != null
                        ? activite.getObjectifSpecifique().getCode()
                        : null)
                .objectifDesignation(
                        activite.getObjectifSpecifique() != null
                        ? activite.getObjectifSpecifique().getDesignation()
                        : null)
                .objectifAnnee(
                        activite.getObjectifSpecifique() != null
                        ? activite.getObjectifSpecifique().getAnnee()
                        : null)
                .idTypeActivite(
                        activite.getTypeActivite() != null
                        ? activite.getTypeActivite().getIdTypeActivite()
                        : null)
                .idSite(
                        activite.getSite() != null
                        ? activite.getSite().getIdSite()
                        : null)
                .idPriorite(activite.getPriorite().getIdPriorite())
                .idService(activite.getService().getIdService())
                .statut(statut.getStatutCode())
                .sousActivites(sousActivites)
                .resultatsIntermediaires(resultatsIntermediaires)
                .build();
    }

    // ------------------------------------------------------------------
    // Ecriture
    // ------------------------------------------------------------------
    /**
     * Creer une activite en brouillon.
     */
    @Transactional
    public ActiviteEcritureResponse creer(ActiviteEcritureRequest request) {

        Utilisateur auteur = activiteService.utilisateurCourant();

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        validerDates(request.getDateDebutPrevue(), request.getDateFinPrevue());

        ObjectifSpecifique objectif = resoudreObjectif(request);

        String reference = resoudreReference(request);

        // Unicite du code verifiee avant l'insert : UNIQUE(code) sur activite
        // est global, et une violation remonterait en 500.
        if (activiteRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new ApiException(
                    "Ce code d'activité est déjà utilisé",
                    HttpStatus.BAD_REQUEST);
        }

        Activite activite = activiteRepository.saveAndFlush(Activite.builder()
                .code(request.getCode().trim())
                .reference(reference)
                .designation(request.getDesignation().trim())
                .dateDebutPrevue(request.getDateDebutPrevue())
                .dateFinPrevue(request.getDateFinPrevue())
                .objectifSpecifique(objectif)
                .typeActivite(resoudreTypeActivite(request.getIdTypeActivite()))
                .site(resoudreSite(request.getIdSite()))
                .priorite(resoudrePriorite(request.getIdPriorite()))
                .service(resoudreService(perimetre, request.getIdService()))
                .build());

        // Premiere entree d historique : c'est elle qui cree le statut
        // courant, puisque activite n'en porte pas. Sans cette ligne,
        // l'activite n'aurait aucun statut et n'apparaitrait dans aucune
        // liste, pas meme parmi les brouillons.
        activiteRepository.ajouterChangementStatut(
                activite.getIdActivite(),
                auteur.getIdUtilisateur(),
                StatutsActivite.BROUILLON,
                "Création de l'activité");

        List<SousActivite> sousActivites = synchroniserSousActivites(
                activite,
                request.getSousActivites(),
                List.of());

        int nombreResultats = remplacerResultatsIntermediaires(
                activite,
                request.getResultatsIntermediaires());

        return reponse(activite, sousActivites.size(), nombreResultats);
    }

    /**
     * Modifier une activite restee en brouillon.
     *
     * 404 si elle n'existe pas, 403 si elle sort du perimetre, 409 si elle
     * n'est plus un brouillon. Les trois se distinguent parce qu'ils ne se
     * corrigent pas de la meme facon : faute de frappe, donnee qui n'est pas
     * sienne, ou onglet ouvert sur une activite deja soumise.
     */
    @Transactional
    public ActiviteEcritureResponse modifier(
            Integer idActivite,
            ActiviteEcritureRequest request) {

        validerId(idActivite);

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        // Verrou pris avant toute relecture du statut.
        Activite activite = activiteRepository.verrouillerActivite(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        StatutCourantRow statut = statutCourant(idActivite, perimetre)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        if (!StatutsActivite.BROUILLON.equals(statut.getStatutCode())) {
            throw new ApiException(
                    "Seule une activité en brouillon peut être modifiée",
                    HttpStatus.CONFLICT);
        }

        validerDates(request.getDateDebutPrevue(), request.getDateFinPrevue());

        ObjectifSpecifique objectif = resoudreObjectif(request);

        String nouveauCode = request.getCode().trim();

        String reference = resoudreReference(request);

        // L'unicite ne se revalide que si le code change : le renvoyer
        // inchange, avec sa propre ligne, renverrait "déjà utilisé".
        if (!nouveauCode.equalsIgnoreCase(activite.getCode())
                && activiteRepository.existsByCodeIgnoreCase(nouveauCode)) {
            throw new ApiException(
                    "Ce code d'activité est déjà utilisé",
                    HttpStatus.BAD_REQUEST);
        }

        List<SousActivite> existantes = sousActiviteRepository
                .findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
                        activite.getIdActivite());

        List<SousActiviteEcritureRequest> demandees = request.getSousActivites() == null
                ? List.of()
                : request.getSousActivites();

        activite.setCode(nouveauCode);
        activite.setReference(reference);
        activite.setDesignation(request.getDesignation().trim());
        activite.setDateDebutPrevue(request.getDateDebutPrevue());
        activite.setDateFinPrevue(request.getDateFinPrevue());
        activite.setObjectifSpecifique(objectif);
        activite.setTypeActivite(resoudreTypeActivite(request.getIdTypeActivite()));
        activite.setSite(resoudreSite(request.getIdSite()));
        activite.setPriorite(resoudrePriorite(request.getIdPriorite()));
        activite.setService(resoudreService(perimetre, request.getIdService()));

        activiteRepository.saveAndFlush(activite);

        List<SousActivite> sousActivites = synchroniserSousActivites(
                activite,
                request.getSousActivites(),
                existantes);

        int nombreResultats = remplacerResultatsIntermediaires(
                activite,
                request.getResultatsIntermediaires());

        return reponse(activite, sousActivites.size(), nombreResultats);
    }

    /**
     * Synchroniser les livrables d'une sous-activite.
     *
     * REECRITURE, ET NON SYNCHRONISATION LIGNE A LIGNE
     *
     * Contrairement aux sous-activites, dont les identifiants sont lus par
     * affectations, livrables et avancements, un livrable n'est designe par
     * personne d'autre : le remplacer entier evite d'avoir a reconcilier des
     * identifiants que le formulaire n'envoie meme pas.
     *
     * MAIS PAS SUR LES LIGNES QUI PORTENT DES FICHIERS
     *
     * C'est la seule reserve, et elle est forte. fichier_sous_activite
     * reference livrable_sous_activite : un depot appartient a l'historique
     * de l'activite et ne peut pas disparaitre parce qu'une ligne de brouillon
     * a ete reecrite. Une ligne existante absente du corps envoye est donc
     * conservee, et le formulaire n'a pas a le savoir : il ne propose pas de
     * supprimer un livrable qui porte des fichiers, et le backend reste
     * verifie meme si le corps en a malgre tout omis un.
     *
     * Un livrable porte par une sous-activite SUPPRIMEE n'a pas ce
     * probleme : il part avec elle, et le garde deja present sur
     * synchroniserSousActivites refuse de supprimer une sous-activite qui
     * porte un livrable. Rien n'est donc supprime en violation de cle
     * etrangere.
     *
     * @param existantes livrables deja en base pour cette sous-activite
     */
    private void synchroniserLivrables(
            SousActivite sousActivite,
            List<LivrableEcritureRequest> demandes,
            List<Livrable> existantes) {

        List<LivrableEcritureRequest> entrees
                = demandes == null ? List.of() : demandes;

        // Designations deja couvertes par une ligne qu'on garde, qu'elle
        // vienne du corps envoye ou d'une ligne protegee par ses fichiers.
        // Sert aussitot a ecarter les doublons : deux livrables de meme
        // designation pour une meme sous-activite doubleraient le detail.
        Set<String> dejaCouverts = new HashSet<>();

        Set<Livrable> aSupprimer = new HashSet<>();

        for (Livrable existante : existantes) {

            String designation = texte(existante.getDesignation());

            boolean encoreDemandee = entrees.stream()
                    .anyMatch(entree -> designation
                            .equalsIgnoreCase(texte(entree.getDesignation())));

            if (encoreDemandee || livrableRepository
                    .compterFichiers(existante.getIdLivrable()) > 0) {
                dejaCouverts.add(designation.toLowerCase());
                continue;
            }

            aSupprimer.add(existante);
        }

        if (!aSupprimer.isEmpty()) {
            livrableRepository.deleteAll(aSupprimer);
        }

        for (LivrableEcritureRequest entree : entrees) {

            String designation = texte(entree.getDesignation());

            if (!dejaCouverts.add(designation.toLowerCase())) {
                continue;
            }

            livrableRepository.save(Livrable.builder()
                    .designation(designation)
                    .description(entree.getDescription() == null
                            || entree.getDescription().isBlank()
                            ? null
                            : entree.getDescription().trim())
                    .sousActivite(sousActivite)
                    .build());
        }
    }

    /**
     * Remplacer l ensemble des resultats intermediaires d une activite.
     *
     * CE N EST PAS UNE SYNCHRONISATION, ET LA DIFFERENCE EST VOLONTAIRE
     *
 * Les sous-activites sont reconciliees ligne a ligne parce que des
     * affectations, livrables et avancements y sont rattaches, qu une
     * suppression ferait disparaitre. Une ligne de resultat intermediaire
     * n'est attachee qu'a son activite : aucune table ne la reference, donc
     * la supprimer n'emporte rien et son identifiant n'est lu par personne.
     * Reecrire l'ensemble est alors plus simple et plus exact qu'une
     * reconciliation, et le formulaire n'a pas a renvoyer d'identifiant.
     *
     * L'ordre de suppression puis d'insertion est impose par la transaction :
     * inserer avant de supprimer echouerait sur la cle etrangere de
     * l'activite si elle vient d'etre creee, et surtout laisserait trainer
     * des lignes qui ne sont plus dans le corps envoye.
     *
     * Les lignes vides sont refusees, et non retirees en silence :
     * @NotBlank sur ResultatIntermediaireEcritureRequest rejette deja le corps
     * en 400, et une ligne vide que l'on ecarterait discrètement ferait
     * disparaitre une saisie sans rien signaler. Le formulaire, lui, ne les
     * envoie pas : il ne propose une ligne qu'a partir du moment ou elle
     * porte une designation.
     *
     * @return nombre de resultats intermediaires enregistres
     */
    private int remplacerResultatsIntermediaires(
            Activite activite,
            List<ResultatIntermediaireEcritureRequest> demandes) {

        resultatIntermediaireRepository
                .deleteAll(resultatIntermediaireRepository
                        .findByActiviteIdActiviteOrderByIdResultatIntermediaireAsc(
                                activite.getIdActivite()));

        if (demandes == null || demandes.isEmpty()) {
            return 0;
        }

        List<ResultatIntermediaire> aEnregistrer = demandes.stream()
                .map(demande -> ResultatIntermediaire.builder()
                        .designation(demande.getDesignation().trim())
                        .activite(activite)
                        .build())
                .toList();

        resultatIntermediaireRepository.saveAll(aEnregistrer);

        return aEnregistrer.size();
    }

    private List<SousActivite> synchroniserSousActivites(
            Activite activite,
            List<SousActiviteEcritureRequest> demandees,
            List<SousActivite> existantes) {

        List<SousActiviteEcritureRequest> entrees
                = demandees == null ? List.of() : demandees;

        Set<Integer> identifiantsExistants = new HashSet<>();

        // Codes de chaque ligne existante, pour distinguer "ce code
        // est le mien" de "ce code est celui d'une voisine" : les deux
        // figurent dans le meme ensemble, mais seul le second est conflit.
        Map<Integer, String> codeParLigne = new HashMap<>();

        // Codes pris dans cette activite, ou revendiques plus haut dans ce
        // meme formulaire. Le code d'une ligne qui va disparaitre y reste :
        // Hibernate insere avant de supprimer au flush, donc le reutiliser
        // dans la meme requete echouerait sur l'unicite. Le refuser ici
        // donne un message clair plutot qu'une erreur de contrainte.
        Set<String> codesPris = new HashSet<>();

        // Le prefixe n'est recherche que si au moins une ligne doit recevoir
        // un code genere, et il se deduit du code de l'activite, donc sans
        // requete. Il ne pouvait pas en dependre autrement.
        String prefixe = entrees.stream()
                .anyMatch(entree -> entree.getCode() == null
                        || entree.getCode().isBlank())
                ? prefixeSousActivite(activite)
                : null;

        existantes.forEach(existante -> {
            identifiantsExistants.add(existante.getIdSousActivite());
            codeParLigne.put(existante.getIdSousActivite(), existante.getCode());
            codesPris.add(majuscule(existante.getCode()));
        });

        List<SousActivite> synchronisees = new ArrayList<>();

        Set<Integer> conserves = new HashSet<>();

        for (SousActiviteEcritureRequest entree : entrees) {

            if (entree.getIdSousActivite() != null
                    && !identifiantsExistants.contains(entree.getIdSousActivite())) {
                throw new ApiException(
                        "Cette sous-activité n'appartient pas à l'activité",
                        HttpStatus.BAD_REQUEST);
            }

            // Deux lignes pour la meme sous-activite : la derniere ecraserait
            // la premiere, silencieusement. Le refus est explicite.
            if (entree.getIdSousActivite() != null
                    && !conserves.add(entree.getIdSousActivite())) {
                throw new ApiException(
                        "Cette sous-activité apparaît plusieurs fois dans le"
                        + " formulaire",
                        HttpStatus.BAD_REQUEST);
            }

            validerDates(entree.getDateDebutPrevue(), entree.getDateFinPrevue());

            String code = resoudreCodeSousActivite(
                    entree, prefixe, codeParLigne, codesPris);

            codesPris.add(majuscule(code));

            if (entree.getIdSousActivite() == null) {

                SousActivite creee = sousActiviteRepository.save(SousActivite.builder()
                        .code(code)
                        .designation(entree.getDesignation().trim())
                        .dateDebutPrevue(entree.getDateDebutPrevue())
                        .dateFinPrevue(entree.getDateFinPrevue())
                        .activite(activite)
                        .build());

                // Une sous-activite neuve n'a aucun livrable existant : le
                // synchroniser n'est donc qu'une creation. save() lui ayant
                // deja donne son identifiant, la ligne de livrable peut
                // pointer dessus dans la meme transaction.
                synchroniserLivrables(creee, entree.getLivrables(), List.of());

                synchronisees.add(creee);

                continue;
            }

            SousActivite existante = existantes.stream()
                    .filter(enregistree -> enregistree.getIdSousActivite()
                            .equals(entree.getIdSousActivite()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(
                    "Cette sous-activité n'appartient pas à l'activité",
                    HttpStatus.BAD_REQUEST));

            codeParLigne.put(existante.getIdSousActivite(), code);

            conserves.add(existante.getIdSousActivite());

            // Les livrables sont synchronises AVANT toute sortie de boucle, et
            // meme quand la sous-activite n'a pas change : ajouter un livrable
            // ne modifie ni son code, ni sa designation, ni ses dates. Un
            // court-circuit place apres ce test perdrait l'ajout, et
            // l'utilisateur verrait son bouton n'avoir rien fait.
            synchroniserLivrables(
                    existante,
                    entree.getLivrables(),
                    livrableRepository
                            .findBySousActiviteIdSousActiviteOrderByIdLivrableAsc(
                                    existante.getIdSousActivite()));

            if (!sousActiviteModifiee(existante, entree, code)) {
                synchronisees.add(existante);
                continue;
            }

            // Les dates reelles ne sont pas reecrites : elles appartiennent
            // au suivi, et une redaction ne doit pas ecraser une execution.
            existante.setCode(code);
            existante.setDesignation(entree.getDesignation().trim());
            existante.setDateDebutPrevue(entree.getDateDebutPrevue());
            existante.setDateFinPrevue(entree.getDateFinPrevue());

            synchronisees.add(sousActiviteRepository.save(existante));
        }

        for (SousActivite existante : existantes) {

            if (conserves.contains(existante.getIdSousActivite())) {
                continue;
            }

            long dependances = sousActiviteRepository.compterDependances(
                    existante.getIdSousActivite());

            if (dependances > 0) {
                throw new ApiException(
                        "La sous-activité « " + existante.getCode() + " » porte"
                        + " encore une affectation, un livrable ou un"
                        + " avancement et ne peut pas être supprimée",
                        HttpStatus.CONFLICT);
            }

            sousActiviteRepository.delete(existante);
        }

        return synchronisees;
    }

    private static String majuscule(String valeur) {
        return valeur == null ? null : valeur.toUpperCase();
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private static boolean sousActiviteModifiee(
            SousActivite existante,
            SousActiviteEcritureRequest demandee,
            String codeResolu) {

        if (demandee == null) {
            return false;
        }

        boolean memeCode = codeResolu != null
                ? codeResolu.equalsIgnoreCase(existante.getCode())
                : Objects.equals(
                        texte(demandee.getCode()),
                        texte(existante.getCode()));

        return !memeCode
                || !Objects.equals(
                        texte(demandee.getDesignation()),
                        texte(existante.getDesignation()))
                || !Objects.equals(
                        demandee.getDateDebutPrevue(), existante.getDateDebutPrevue())
                || !Objects.equals(
                        demandee.getDateFinPrevue(), existante.getDateFinPrevue());
    }

    /**
     * Code d'une sous-activite : celui saisi, ou un code genere.
     *
     * UNIQUE porte sur toute la table, pas seulement par activite, et la
     * colonne est VARCHAR(20). Un code saisi doit donc etre libre dans la base
     * entiere, a moins qu'il ne soit deja celui de la ligne modifiee. Sans
     * cette distinction, prendre le code d'une voisine serait accepte ici puis
     * refuse par la base en erreur de contrainte, donc remontee en 500 alors
     * qu'elle releve d'une saisie.
     *
     * Sinon un code est genere dans le format du jeu de donnees,
     * SA-ANNEE-RANG-SEQUENCE : demander a l'utilisateur d'inventer un code
     * unique global n'apporterait rien.
     *
     * Le prefixe est recu et non recalcule ici, parce qu'il ne change pas
     * d'une ligne a l'autre : le compiler une fois par enregistrement evite
     * une requete par sous-activite, ce qui transformerait un nombre de
     * requetes fixe en une requete par ligne.
     */
    private String resoudreCodeSousActivite(
            SousActiviteEcritureRequest entree,
            String prefixe,
            Map<Integer, String> codeParLigne,
            Set<String> codesPris) {

        if (entree.getCode() != null && !entree.getCode().isBlank()) {

            String code = entree.getCode().trim();

            if (code.length() > 20) {
                throw new ApiException(
                        "Le code d'une sous-activité ne doit pas dépasser"
                        + " 20 caractères",
                        HttpStatus.BAD_REQUEST);
            }

            if (estLibre(code, entree.getIdSousActivite(), codeParLigne, codesPris)) {
                return code;
            }

            throw new ApiException(
                    "Ce code de sous-activité est déjà utilisé",
                    HttpStatus.BAD_REQUEST);
        }

        for (int numero = 1; numero <= 99; numero++) {

            String candidat = prefixe + String.format("%02d", numero);

            if (estLibre(candidat, entree.getIdSousActivite(), codeParLigne, codesPris)) {
                return candidat;
            }
        }

        throw new ApiException(
                "Impossible de générer un code de sous-activité pour cette"
                + " activité",
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Prefixe des codes generes pour les sous-activites d'une activite :
     * le code de l activite, prefixe de SA-, suivi d un tiret.
     *
     * LE CODE DE L ACTIVITE, ET NON SON RANG
     *
     * Le prefixe etait le rang GLOBAL de l activite, compte sur les
     * identifiants. Il garantissait l unicite, mais rendait le code impossible
     * a prevoir : le rang n existe qu apres enregistrement, donc le
     * formulaire ne pouvait proposer qu un code vide, et l utilisateur ne
     * découvrait SA-2027-07-01 qu en relisant l activite creee. Une valeur
     * que l application sait calculer ne doit pas etre devinee.
     *
     * A-<annee>-<objectif>-<n> pour un PTA, A-<annee>-<n> pour un NON PTA :
     * dans les deux cas le code de l activite identifie deja l activite de
     * facon unique, et les codes d activite sont eux-memes uniques. Le
     * prefixe qui en derive l est donc, et y ajouter la sequence de la
     * sous-activite conserve l unicite.
     *
     * CE QUI REMPLACE LE RANG NE CHANGE PAS AUX LIGNES EXISTANTES
     *
     * Seul un code GENERE change de forme. Les sous-activites deja enregistrees
     * gardent le leur, envoye tel quel, et le jeu de donnees reste melange :
     * SA-2027-03-01 pour une ancienne ligne, SA-2027-01-01-01 pour une
     * nouvelle. Reconcilier les anciennes obligerait a les renumeroter, donc
     * a modifier des lignes que personne n'a demandees de toucher.
     *
     * Un code qui ne ressemble pas a A-... ne produit pas de prefixe : le
     * backend genere alors avec le rang, plutot que de fabriquer un prefixe
     * sans queue. C est le seul cas ou l ancienne regle subsiste, et il ne
     * devrait pas se produire : le formulaire controle la forme avant de
     * proposer.
     */
    private String prefixeSousActivite(Activite activite) {

        String code = activite.getCode() == null ? "" : activite.getCode().trim();

        // Le code de l'activite ne sert que pour un PTA, et la raison est
        // mesuree : un PTA a quatre groupes (A-2027-01-02), donc le code de sa
        // sous-activite en a cinq -- SA-2027-01-02-01 -- et aucune des 400
        // lignes du jeu de donnees n'en a plus de trois. La forme est donc
        // libre par construction.
        //
        // Une NON PTA n'a que trois groupes (A-2027-51), donc sa sous-activite
        // en aurait trois aussi, SA-2027-51-01 : c'est exactement la forme du
        // rang global, et ce prefixe est deja porte par cinq lignes du jeu de
        // donnees. La proposer serait refused a l'enregistrement. Le rang est
        // donc conserve pour elle, seul moyen d'y produire du libre.
        //
        // Consequence assumee : les sous-activites d'une NON PTA gardent un code
        // que le formulaire ne peut pas prevoir, puisqu'il n'a pas d'objectif.
        // C'est le prix de codes previsibles la ou elles le sont, et libres la
        // ou elles ne le seraient pas.
        if (activite.getObjectifSpecifique() != null
                && code.length() > 2
                && code.charAt(0) == 'A'
                && code.charAt(1) == '-') {
            return "SA-" + code.substring(2) + "-";
        }

        int rang = activiteRepository.compterActivitesJusqua(
                activite.getIdActivite());

        return "SA-" + activite.getDateDebutPrevue().getYear()
                + "-" + String.format("%02d", rang) + "-";
    }

    /**
     * Un code est libre s'il n'appartient a aucune autre ligne.
     *
     * Deux cas se confondent si l'on regarde seulement "le code est-il pris" :
     * un code deja porte par la ligne que l'on modifie est le sien, pas une
     * collision. C'est ce qui permet de renvoyer un code inchange sans etre
     * refuse pour autant.
     */
    private boolean estLibre(
            String code,
            Integer idLigne,
            Map<Integer, String> codeParLigne,
            Set<String> codesPris) {

        if (codesPris.contains(majuscule(code))) {

            String codeDeMaLigne
                    = idLigne == null ? null : codeParLigne.get(idLigne);

            return codeDeMaLigne != null
                    && codeDeMaLigne.equalsIgnoreCase(code);
        }

        return !sousActiviteRepository.existsByCodeIgnoreCase(code);
    }

    // ------------------------------------------------------------------
    // Resolution des referentiels
    // ------------------------------------------------------------------
    /**
     * Service cible.
     *
     * Un utilisateur rattache a un service ne choisit pas : c'est le sien. Ce
     * n'est pas une commodite mais une regle d'acces -- il n'a d'ailleurs aucun
     * moyen de connaitre l'identifiant de son propre service, options.services
     * etant vide pour lui, et la valeur qu'il enverrait serait de toute facon
     * remplacee.
     *
     * Un utilisateur de niveau departement choisit, mais parmi les seuls
     * services actifs de son departement : lui en proposer un autre reviendrait
     * a lui permettre d'ecrire hors de son perimetre.
     *
     * Le type est nomme en entier parce que mg.bank.backend.model.Service et
     * org.springframework.stereotype.Service portent le meme nom simple ; c'est
     * la convention deja suivie dans UtilisateurService.
     */
    private mg.bank.backend.model.Service resoudreService(
            PerimetreUtilisateur perimetre,
            Integer idServiceRecu) {

        if (perimetre.estRattacheService()) {
            return serviceRepository.findById(perimetre.getIdService())
                    .orElseThrow(() -> new ApiException(
                    "Votre service n'existe plus",
                    HttpStatus.FORBIDDEN));
        }

        if (idServiceRecu == null) {
            throw new ApiException(
                    "Le service est obligatoire",
                    HttpStatus.BAD_REQUEST);
        }

        mg.bank.backend.model.Service service = serviceRepository.findById(idServiceRecu)
                .orElseThrow(() -> new ApiException(
                "Le service sélectionné n'existe pas",
                HttpStatus.BAD_REQUEST));

        if (!Boolean.TRUE.equals(service.getActif())) {
            throw new ApiException(
                    "Le service sélectionné n'est pas actif",
                    HttpStatus.BAD_REQUEST);
        }

        if (perimetre.estNiveauDepartement()
                && !perimetre.getIdDepartement().equals(
                        service.getDepartement().getIdDepartement())) {
            throw new ApiException(
                    "Le service sélectionné n'appartient pas à votre"
                    + " département",
                    HttpStatus.FORBIDDEN);
        }

        return service;
    }

    /**
     * Objectif specifique, avec la coherence PTA / NON PTA verifiee.
     *
     * Le drapeau pta et l'objectif se contredisent rarement, mais ils peuvent :
     * un corps qui annonce PTA sans objectif, ou un corps qui annonce NON PTA
     * en adossant l'activite a un objectif. Les deux sont refuses plutot que
     * corriges en silence, pour qu'aucune activite PTA n'existe sans objectif,
     * et reciproquement.
     */
    private ObjectifSpecifique resoudreObjectif(ActiviteEcritureRequest request) {

        boolean pta = Boolean.TRUE.equals(request.getPta());
        Integer idObjectif = request.getIdObjectifSpecifique();

        if (pta && idObjectif == null) {
            throw new ApiException(
                    "Une activité PTA doit être rattachée à un objectif"
                    + " spécifique",
                    HttpStatus.BAD_REQUEST);
        }

        if (!pta && idObjectif != null) {
            throw new ApiException(
                    "Une activité non PTA ne peut pas être rattachée à un"
                    + " objectif spécifique",
                    HttpStatus.BAD_REQUEST);
        }

        if (idObjectif == null) {
            return null;
        }

        return objectifSpecifiqueRepository.findById(idObjectif)
                .orElseThrow(() -> new ApiException(
                "L'objectif spécifique sélectionné n'existe pas",
                HttpStatus.BAD_REQUEST));
    }

    /**
     * Reference de l'activite, ou null pour une NPTA.
     *
     * Meme sens de lecture que resoudreObjectif, et pour la meme raison : la
     * reference designe l objectif de rattachement, donc elle ne peut etre
     * presente que la ou cet objectif existe.
     *
     * Les deux sens sont traites, mais pas de la meme facon, et l 'asymetrie
     * est deliberee.
     *
     * PTA sans reference : refus. Une activite rattachee a un objectif et
     * depourvue de la reference qui la rattache perdrait le seul moyen de la
     * relier a cet objectif dans un document imprime, et le default
     * @NotBlank withdrawn plus tot laissait passer ce cas en silence.
     *
     * NPTA avec reference : remise a null, sans refus. La valeur ne
     * designe rien, et surtout le client n'a rien demande de faux : il a
     * rempli le champ que le formulaire affiche. Le refuser transformerait un
     * detail de saisie en erreur, pour un corps par ailleurs valide. Le
     * clearest sans rien corriger, c'est de retenir ce qui est vrai : pas
     * d 'objectif, pas de reference.
     */
    private String resoudreReference(ActiviteEcritureRequest request) {

        String reference = request.getReference() == null
                ? ""
                : request.getReference().trim();

        if (Boolean.TRUE.equals(request.getPta())) {

            if (reference.isEmpty()) {
                throw new ApiException(
                        "Une activité PTA doit porter une référence",
                        HttpStatus.BAD_REQUEST);
            }

            return reference;
        }

        // Le test sur le contenu est sans objet ici : dans les deux cas le
        // resultat est null. Ecrire "reference.isEmpty() ? null : null" pour
        // le montrer serait trompeur, la variable n existe plus.
        return null;
    }

    private Priorite resoudrePriorite(Integer idPriorite) {

        Priorite priorite = prioriteRepository.findById(idPriorite)
                .orElseThrow(() -> new ApiException(
                "La priorité sélectionnée n'existe pas",
                HttpStatus.BAD_REQUEST));

        if (!Boolean.TRUE.equals(priorite.getActif())) {
            throw new ApiException(
                    "La priorité sélectionnée n'est pas active",
                    HttpStatus.BAD_REQUEST);
        }

        return priorite;
    }

    private TypeActivite resoudreTypeActivite(Integer idTypeActivite) {

        if (idTypeActivite == null) {
            return null;
        }

        TypeActivite typeActivite = typeActiviteRepository.findById(idTypeActivite)
                .orElseThrow(() -> new ApiException(
                "Le type d'activité sélectionné n'existe pas",
                HttpStatus.BAD_REQUEST));

        if (!Boolean.TRUE.equals(typeActivite.getActif())) {
            throw new ApiException(
                    "Le type d'activité sélectionné n'est pas actif",
                    HttpStatus.BAD_REQUEST);
        }

        return typeActivite;
    }

    private Site resoudreSite(Integer idSite) {

        if (idSite == null) {
            return null;
        }

        Site site = siteRepository.findById(idSite)
                .orElseThrow(() -> new ApiException(
                "Le site sélectionné n'existe pas",
                HttpStatus.BAD_REQUEST));

        if (!Boolean.TRUE.equals(site.getActif())) {
            throw new ApiException(
                    "Le site sélectionné n'est pas actif",
                    HttpStatus.BAD_REQUEST);
        }

        return site;
    }

    // ------------------------------------------------------------------
    // Verifications transverses
    // ------------------------------------------------------------------
    /**
     * fin >= debut, des deux cotes.
     *
     * Meme regle que chk_activite_dates_prevues et
     * chk_sous_activite_dates_prevues, mais rendue en 400 lisible : sans elle,
     * une fin anterieure au debut remonterait en 500 avec un message de
     * contrainte SQL illisible.
     */
    private void validerDates(LocalDate debut, LocalDate fin) {

        if (debut == null) {
            throw new ApiException(
                    "La date de début prévue est obligatoire",
                    HttpStatus.BAD_REQUEST);
        }

        if (fin != null && fin.isBefore(debut)) {
            throw new ApiException(
                    "La date de fin prévue ne peut pas précéder la date de"
                    + " début prévue",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validerId(Integer idActivite) {

        if (idActivite == null || idActivite < 1) {
            throw new ApiException(
                    "L'identifiant de l'activité doit être un entier positif",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Optional<StatutCourantRow> statutCourant(
            Integer idActivite,
            PerimetreUtilisateur perimetre) {

        return activiteRepository.findStatutCourant(
                idActivite,
                perimetre.estRattacheService() ? perimetre.getIdService() : null,
                perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null);
    }

    private ActiviteEcritureResponse reponse(
            Activite activite,
            int nombreSousActivites,
            int nombreResultatsIntermediaires) {
        return ActiviteEcritureResponse.builder()
                .idActivite(activite.getIdActivite())
                .code(activite.getCode())
                .reference(activite.getReference())
                .designation(activite.getDesignation())
                .statut(StatutsActivite.BROUILLON)
                .dateDebutPrevue(activite.getDateDebutPrevue())
                .dateFinPrevue(activite.getDateFinPrevue())
                .dateEnregistrement(LocalDateTime.now())
                .nombreSousActivites(nombreSousActivites)
                .nombreResultatsIntermediaires(nombreResultatsIntermediaires)
                .build();
    }

    private SousActiviteFormulaireDTO versFormulaireSousActivite(SousActivite sous) {
        return SousActiviteFormulaireDTO.builder()
                .idSousActivite(sous.getIdSousActivite())
                .code(sous.getCode())
                .designation(sous.getDesignation())
                .dateDebutPrevue(sous.getDateDebutPrevue())
                .dateFinPrevue(sous.getDateFinPrevue())
                .dateDebutReelle(sous.getDateDebutReelle())
                .dateFinReelle(sous.getDateFinReelle())
                .livrables(livrableRepository
                        .findBySousActiviteIdSousActiviteOrderByIdLivrableAsc(
                                sous.getIdSousActivite())
                        .stream()
                        .map(this::versFormulaireLivrable)
                        .toList())
                .build();
    }

    private LivrableFormulaireDTO versFormulaireLivrable(Livrable livrable) {
        return LivrableFormulaireDTO.builder()
                .idLivrable(livrable.getIdLivrable())
                .designation(livrable.getDesignation())
                .description(livrable.getDescription())
                .nombreFichiers(livrableRepository.compterFichiers(
                        livrable.getIdLivrable()))
                .build();
    }

    private ResultatIntermediaireFormulaireDTO versFormulaireResultatIntermediaire(
            ResultatIntermediaire resultat) {
        return ResultatIntermediaireFormulaireDTO.builder()
                .idResultatIntermediaire(resultat.getIdResultatIntermediaire())
                .designation(resultat.getDesignation())
                .build();
    }
}
