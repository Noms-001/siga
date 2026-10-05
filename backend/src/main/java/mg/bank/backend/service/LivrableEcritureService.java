package mg.bank.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.LivrableAjoutRequest;
import mg.bank.backend.dto.LivrableDetailDTO;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Fichier;
import mg.bank.backend.model.Livrable;
import mg.bank.backend.model.SousActivite;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.FichierRepository;
import mg.bank.backend.repository.LivrableRepository;
import mg.bank.backend.repository.SousActiviteRepository;
import mg.bank.backend.repository.projection.SousActiviteDetailRow;

/**
 * Depot d un livrable et de ses fichiers depuis le detail d une sous-activite.
 *
 * CE SERVICE EST SEULEMENT CELUI DU SUIVI
 *
 * La redaction d une activite -- sa declaration des livrables, au moment ou
 * elle est encore un brouillon -- appartient a ActiviteEcritureService, qui
 * ecrit tout le lot dans la transaction de l activite. Ce service ne fait que
 * ce qui n existe pas de ce chemin-la : ajouter un livrable a une activite
 * deja publiee, donc deposer une piece au suivi. Deux chemins, deux services :
 * aucune des regles ci-dessous n a a etre reecrite dans l autre.
 *
 * LA REGLE QUI COMMANDE : BROUILLON OU EN COURS
 *
 * Deux statuts, et deux raisons de les admettre -- ce qui n est pas la meme
 * chose que deux synonymes.
 *
 * EN_COURS : c etait la regle demandee initialement, et elle tient. Un livrable
 * est la preuve qu un travail a produit quelque chose ; le deposer apres que
 * l activite soit terminee, annulee ou reportee reviendrait a faire dire a la
 * piece qu elle etait la quand elle ne l etait pas.
 *
 * BROUILLON : c est la phase ou l activite se COMPOSE, et la seule ou ses
 * sous-activites existent. Les sous-activites ne s ecrient que par
 * ActiviteEcritureService -- synchroniserSousActivites, appele depuis creer et
 * modifier -- et modifier refuse tout statut autre que BROUILLON ("Seule une
 * activite en brouillon peut etre modifiee"). Une sous-activite est donc, par
 * construction, un brouillon ou un vestige de l epoque ou l activite en etait
 * un.
 *
 * Refuser le depot en BROUILLON rendrait la regle absurde dans les deux sens :
 * on ne pourrait pas joindre de livrable a la sous-activite qu on vient de
 * creer, et une sous-activite d une activite dont le statut a change n en
 * accepterait plus jamais. Le brouillon est aussi un etat durable : rien dans
 * le code ne ramene une activite vers BROUILLON apres sa creation, et rien ne
 * l en fait sortir non plus.
 *
 * CE QUI RESTE REFUSE : tout ce qui clot ou fige la vie de l activite
 * (TERMINEE, ANNULEE, REPORTEE), les statuts non publies
 * (EN_ATTENTE_VALIDATION, REJETE, VALIDEE) et son report (EN_RETARD), plus la
 * suspension et l absence de statut courant. Une activite sans historique n a
 * pas de statut : le depot est refuse, defaut prudent, l inverse autoriserait
 * un depot sur une activite dont on ignore l etat.
 *
 * Le controle porte sur le statut courant lu dans l historique, sous verrou, et
 * non sur une valeur venue avec la requete : celle-ci serait forgeable, et le
 * serait d autant plus qu elle decide d une ecriture.
 */
@Service
@RequiredArgsConstructor
public class LivrableEcritureService {

    /**
     * Racine des depots, hors du classpath.
     *
     * Valeur par defaut = celle du jeu de donnees, qui ecrit deja sous
     * /data/saga/livrables. Reprendre le meme repertoire fait qu un fichier
     * televerse et un fichier du jeu de donnees se lisent par exactement le
     * meme chemin, et qu un volume monte sur ce repertoire retrouve les deux.
     *
     * Surchargeable pour un poste de developpement, ou ce chemin n est ni
     * accessible ni desirable.
     */
    @Value("${app.depot.livrables:/data/saga/livrables}")
    private String racineDepot;

    /**
     * Plafond par fichier, en octets (10 Mo par defaut).
     *
     * Le meme plafond est declare dans multipart.max-file-size, qui rejette
     * plus tot, avant que l octet n entre en memoire. Celui-ci ne peut pas
     * dependre du nom de cette propriete -- le lire serait du couplage a une
     * chaine de configuration -- et sert a fermer le trou si le multipart a ete
     * relache.
     */
    @Value("${app.depot.livrables.taille-max:10485760}")
    private long tailleMax;

    /** Nombre de fichiers acceptes pour un livrable. */
    @Value("${app.depot.livrables.nombre-max:10}")
    private int nombreMax;

    private final ActiviteRepository activiteRepository;
    private final SousActiviteRepository sousActiviteRepository;
    private final LivrableRepository livrableRepository;
    private final FichierRepository fichierRepository;
    private final ActiviteService activiteService;
    private final ActiviteDetailService activiteDetailService;

    /**
     * Creer un livrable, et y deposer les fichiers joints.
     *
     * La ligne du livrable et celles des fichiers sont ecrites dans la meme
     * transaction. Les octets, eux, sont sur le disque avant le commit : une
     * transaction qui annule ne sait pas retirer un fichier. Ce qui a deja ete
     * ecrit est donc efface explicitement si la suite echoue, sinon un depot
     * annule laisserait des fichiers orphelins que personne ne pourrait lister
     * -- ils ne seraient rattaches a aucun livrable enregistre.
     */
    @Transactional
    public LivrableDetailDTO creer(
            Integer idActivite,
            Integer idSousActivite,
            LivrableAjoutRequest request,
            MultipartFile[] fichiers) {

        validerIdentifiants(idActivite, idSousActivite);

        SousActiviteDetailRow demandee = exigerDepotAutorise(
                idActivite,
                idSousActivite,
                "Un livrable ne peut être ajouté que sur une activité en brouillon"
                        + " ou en cours");

        String designation = request.getDesignation().trim();

        // La comparaison ignore la casse sur un libelle deja recapte : voir
        // LivrableRepository.existsBySousActiviteIdSousActivite
        // AndDesignationIgnoreCase.
        if (livrableRepository
                .existsBySousActiviteIdSousActiviteAndDesignationIgnoreCase(
                        idSousActivite, designation)) {
            throw new ApiException(
                    "Ce libellé de livrable existe déjà pour cette sous-activité",
                    HttpStatus.BAD_REQUEST);
        }

        SousActivite sousActivite = sousActiviteRepository.findById(idSousActivite)
                .orElseThrow(() -> activiteDetailService
                        .refusOuAbsenceSousActivite(idActivite, idSousActivite));

        String description = request.getDescription() == null
                || request.getDescription().isBlank()
                ? null
                : request.getDescription().trim();

        Livrable livrable = livrableRepository.save(Livrable.builder()
                .designation(designation)
                .description(description)
                .sousActivite(sousActivite)
                .build());

        deposerFichiers(
                livrable, fichiers, activiteService.utilisateurCourant(), 0);

        return LivrableDetailDTO.builder()
                .id(livrable.getIdLivrable())
                .designation(livrable.getDesignation())
                .description(livrable.getDescription())
                // Decrit par la meme lecture que le detail, et non par les
                // lignes que ce service vient d ecrire : la reponse est alors
                // indiscernable d un rechargement, et il n existe qu une
                // description possible d un fichier depose.
                .fichiers(activiteDetailService.fichiers(livrable.getIdLivrable()))
                .build();
    }

    /**
     * Ajouter des fichiers a un livrable deja enregistre.
     *
     * Meme regle d acces que la creation d un livrable -- meme verrou, meme
     * statut autorise, memes plafonds -- et pour la meme raison : deposer un
     * fichier est une ecriture sur l activite, au meme titre qu en declarer un
     * livrable. Autoriser l une des deux sous un statut et l autre non
     * rendrait le depot dependre du bouton presse.
     *
     * Les fichiers deja deposes ne sont ni remplaces ni supprimes : le depot
     * s ajoute. C est ce qui impose deux precautions, traitees plus bas -- le
     * plafond compte le total, et les noms sur disque ne recommencent pas a
     * zero.
     */
    @Transactional
    public LivrableDetailDTO ajouterFichiers(
            Integer idActivite,
            Integer idSousActivite,
            Integer idLivrable,
            MultipartFile[] fichiers) {

        validerIdLivrable(idLivrable);
        validerIdentifiants(idActivite, idSousActivite);

        exigerDepotAutorise(
                idActivite,
                idSousActivite,
                "Des fichiers ne peuvent être déposés que sur une activité en"
                        + " brouillon ou en cours");

        // Le livrable doit etre celui de CETTE sous-activite. findSousActivite
        // a deja valide les deux premiers identifiants et le perimetre ; sans
        // ce troisieme controle, un identifiant de livrable passe en parametre
        // pourrait viser un livrable d une autre sous-activite du meme
        // perimetre.
        Livrable livrable = livrableRepository
                .findByIdLivrableAndSousActiviteIdSousActivite(
                        idLivrable, idSousActivite)
                .orElseThrow(() -> new ApiException(
                        "Livrable introuvable pour cette sous-activité",
                        HttpStatus.NOT_FOUND));

        // Lu sous le meme verrou que le statut : deux ajouts simultanes
        // verraient sinon le meme compte, et le plafond serait franchi par les
        // deux.
        int dejaDeposes = (int) livrableRepository.compterFichiers(idLivrable);

        deposerFichiers(
                livrable, fichiers, activiteService.utilisateurCourant(), dejaDeposes);

        return LivrableDetailDTO.builder()
                .id(livrable.getIdLivrable())
                .designation(livrable.getDesignation())
                .description(livrable.getDescription())
                .fichiers(activiteDetailService.fichiers(idLivrable))
                .build();
    }

    /**
     * Perimetre, verrou, et statut autorisant un depot.
     *
     * Factorise pour que la regle d acces soit ecrite une seule fois : creer un
     * livrable et ajouter des fichiers verifient les memes choses dans le meme
     * ordre. Les dupliquer autoriserait un des deux chemins a oublier le
     * verrou, ou a appliquer une liste de statuts differente -- et le second
     * cas ne se verrait qu a l usage, sur un statut que personne n essaye.
     *
     * Le message est fourni par l appelant parce que l operation refusee n est
     * pas la meme : ajouter un livrable a une activite terminee et y joindre
     * un fichier sont deux refus qui doivent se nommer correctement.
     *
     * L ordre est deliberé : verrou d'abord, lecture du statut ensuite. Sans
     * le verrou, deux depots simultanes pourraient tous deux lire un statut
     * favorable alors que le premier fait terminer l activite entre-temps.
     */
    private SousActiviteDetailRow exigerDepotAutorise(
            Integer idActivite,
            Integer idSousActivite,
            String messageStatutRefuse) {

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();

        activiteRepository.verrouillerActivite(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        // findSousActivite applique le perimetre et ramene le statut de
        // l activite mere dans la MEME requete : c est cette lecture qui
        // decide de tout le reste, et eviterait sinon deux allers-retours.
        SousActiviteDetailRow demandee = activiteRepository.findSousActivite(
                        idActivite,
                        idSousActivite,
                        perimetre.estRattacheService() ? perimetre.getIdService() : null,
                        perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null)
                .orElseThrow(() -> activiteDetailService
                        .refusOuAbsenceSousActivite(idActivite, idSousActivite));

        if (!depotAutorise(demandee.getStatutActivite())) {
            throw new ApiException(messageStatutRefuse, HttpStatus.CONFLICT);
        }

        return demandee;
    }

    /**
     * Ecrire les fichiers joints, dans la limite des places restantes.
     *
     * Fichiers facultatifs a la creation : un livrable peut etre declare avant
     * que la piece soit prete, et la liste affiche alors "Aucun fichier depose".
     * Le serveur n impose donc rien de ce cote ; il ne refuse que ce qui n est
     * pas deposable.
     *
     * rangDepart EST LE NOMBRE DE FICHIERS DEJA DEPOSES, PAS ZERO
     *
     * Il decale le rang qui nomme le fichier sur disque. Sans ce decalage, un
     * second depot sur le meme livrable produirait livrable_7_v1_0.pdf, deja
     * pris par le premier : le nouvel octet remplacerait l ancien sur le
     * disque, la ligne du premier fichier resterait en base pointant vers ce
     * chemin, et son telechargement rendrait le contenu du second. Deux lignes
     * porteraient des noms et un contenu qui ne se repondent plus.
     *
     * C est aussi pourquoi le plafond porte sur le TOTAL : un livrable ne peut
     * pas porter plus de nombreMax fichiers, qu ils soient venus en un seul
     * envoi ou en dix. Compter chaque envoi separement autoriserait un total
     * sans borne.
     */
    private void deposerFichiers(
            Livrable livrable,
            MultipartFile[] fichiers,
            Utilisateur auteur,
            int rangDepart) {

        if (fichiers == null || fichiers.length == 0) {
            return;
        }

        List<MultipartFile> retenus = new ArrayList<>();

        for (MultipartFile fichier : fichiers) {
            if (fichier != null && !fichier.isEmpty()) {
                retenus.add(fichier);
            }
        }

        if (retenus.size() > nombreMax - rangDepart) {
            throw new ApiException(
                    "Un livrable ne peut pas porter plus de "
                            + nombreMax + " fichiers",
                    HttpStatus.BAD_REQUEST);
        }

        List<Fichier> lignes = new ArrayList<>();
        List<Path> ecrits = new ArrayList<>();

        try {
            for (int i = 0; i < retenus.size(); i++) {
                MultipartFile fichier = retenus.get(i);

                validerFichier(fichier);

                Path ecrit = ecrireUn(fichier, livrable, rangDepart + i);

                ecrits.add(ecrit);

                lignes.add(Fichier.builder()
                        .nomFichier(ecrit.getFileName().toString())
                        .nomOriginal(nomOriginal(fichier))
                        .cheminFichier(ecrit.toAbsolutePath().toString())
                        // Premiere version de CE fichier. La contrainte
                        // CHECK (version > 0) l'impose.
                        //
                        // Un fichier ajoute plus tard reste en version 1 : il
                        // est un fichier nouveau, pas une nouvelle version d'un
                        // fichier existant, et rien ici ne remplace un depot
                        // precedent. Une revision supposerait un endpoint de
                        // remplacement, qui n'existe pas -- et un fichier
                        // remplace devrait garder le chemin de son
                        // remplacant, pas en produire un autre.
                        .version(1)
                        .extension(extension(fichier))
                        .typeMime(fichier.getContentType())
                        .taille(fichier.getSize())
                        .dateDepot(LocalDateTime.now())
                        .utilisateur(auteur)
                        .livrable(livrable)
                        .build());
            }

            fichierRepository.saveAll(lignes);

            // Les lignes doivent etre visibles de la relecture qui decrit la
            // reponse. saveAll ne garantit pas le flush, et la lecture qui
            // suit est native : sans cet appel explicite, la reponse pourrait
            // ne pas montrer les fichiers que l envoi vient de deposer -- la
            // page afficherait "Aucun fichier depose" apres un depot reussi.
            fichierRepository.flush();
        } catch (IOException e) {
            // Le rollback annule les lignes, pas les octets deja ecrits.
            supprimer(ecrits);
            // Traduite comme dans telechargerFichier : une erreur de disque ne
            // doit pas remonter en trace technique, ni laisser croire que la
            // requete etait malformee.
            throw new ApiException(
                    "Enregistrement d'un fichier impossible",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            // Meme nettoyage pour un refus metier survenu apres coup, lui aussi
            // apres avoir ecrit des octets.
            supprimer(ecrits);
            throw e;
        }
    }

    /**
     * Ecrire un fichier et rendre son chemin.
     *
     * Le nom de stockage est construit par ce service -- livrable, version et
     * rang dans le lot -- et JAMAIS repris du nom d origine : un nom d origine
     * peut contenir un chemin ("../../etc/passwd"), des accents, un separateur.
     * Il n entre donc pas sur le disque. Il reste en base pour l affichage et
     * le nom de telechargement, ou il est inoffensif.
     *
     * REPLACE_EXISTING : deux fichiers d un meme lot doivent produire deux
     * noms distincts, ce que garantit l index. Cette option ne sert donc qu a
     * un depot rejoue sur le meme livrable apres un echec, ou a ne pas
     * echouer sur un fichier que cet envoi a lui-meme produit.
     */
    private Path ecrireUn(MultipartFile fichier, Livrable livrable, int rang)
            throws IOException {

        Path dossier = Path.of(racineDepot)
                .resolve(String.valueOf(livrable.getIdLivrable()));

        Files.createDirectories(dossier);

        Path cible = dossier.resolve(nomDeStockage(livrable, fichier, rang));

        try (InputStream entree = fichier.getInputStream()) {
            Files.copy(entree, cible, StandardCopyOption.REPLACE_EXISTING);
        }

        return cible;
    }

    /**
     * Nom sur disque : livrable_<id>_v<version>_<rang>.<extension>.
     *
     * LE RANG EST CE QUI EMPECHE DEUX FICHIERS DE S ECRASER
     *
     * Un lot contient des fichiers de noms et d extensions varies, dont deux
     * PDF -- le cas le plus banal. Sans rang, les deux porteraient le meme nom
     * de stockage et le second remplacerait le premier : la ligne du premier
     * resterait en base, pointant vers un chemin qui ne contient plus son contenu, et le
     * telechargement renverrait le fichier suivant.
     *
     * Le rang est donc la position dans le lot tel qu envoye. Il est unique
     * par construction, et l ordre d envoi est celui que l utilisateur voit.
     */
    private String nomDeStockage(
            Livrable livrable,
            MultipartFile fichier,
            int rang) {

        int version = 1;

        String extension = extension(fichier);

        return "livrable_" + livrable.getIdLivrable()
                + "_v" + version
                + "_" + rang
                + (extension == null || extension.isBlank() ? "" : "." + extension);
    }

    /**
     * Refuser ce qui ne peut pas etre depose.
     *
     * La taille est verifiee ici malgre multipart.max-file-size : cette
     * propriete protege le serveur avant que l octet n entre en memoire, mais
     * elle releve de la configuration et peut etre relachee. Un second
     * controle, ici, ne depend pas d elle.
     */
    private void validerFichier(MultipartFile fichier) {

        if (fichier.getSize() > tailleMax) {
            throw new ApiException(
                    "Un fichier dépasse la taille maximale autorisée",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Nom d origine, borne a la longueur de la colonne.
     *
     * VARCHAR(255) refuserait un nom plus long, et l erreur de base
     * survenue la rendrait en un 500 illisible. Couper est sans risque ici :
     * ce nom sert a l affichage et au nom de telechargement, pas a retrouver
     * le fichier sur le disque.
     */
    private String nomOriginal(MultipartFile fichier) {

        String nom = fichier.getOriginalFilename() == null
                ? ""
                : fichier.getOriginalFilename().trim();

        if (nom.length() > 255) {
            nom = nom.substring(0, 255);
        }

        return nom.isEmpty() ? "fichier" : nom;
    }

    /**
     * Extension en minuscules, ou null.
     *
     * Volontairement derivee du nom d origine et non du type MIME annonce :
     * un navigateur annonce "application/octet-stream" pour une dizaine de
     * formats, alors que l extension sert a l affichage. Aucun point dans le
     * nom ne signifie aucune extension, et il n y en a pas a inventer.
     */
    private String extension(MultipartFile fichier) {

        String nom = fichier.getOriginalFilename() == null
                ? ""
                : fichier.getOriginalFilename();

        int dernierPoint = nom.lastIndexOf('.');

        if (dernierPoint < 0 || dernierPoint == nom.length() - 1) {
            return null;
        }

        String extension = nom.substring(dernierPoint + 1)
                .toLowerCase(Locale.ROOT);

        // VARCHAR(20) : au-dela, ce n'est plus une extension mais un nom.
        return extension.length() > 20 ? null : extension;
    }

    /** Retirer du disque ce qui a ete ecrit, sans qu une erreur y echappe. */
    private void supprimer(List<Path> chemins) {
        for (Path chemin : chemins) {
            try {
                Files.deleteIfExists(chemin);
            } catch (IOException e) {
                // Un fichier orphelin ne peut pas etre signale ici : il n'a pas
                // de ligne en base, donc rien ne dit ou le chercher. Il est
                // laisse en place, ce qui est le seul echappement ; un fichier
                // intact et sans proprietaire est preferable a un echec qui
                // empecherait tout le reste du depot.
            }
        }
    }

    /**
     * Le depot est-il ouvert sur cet statut d activite mere ?
     *
     * Les statuts admis sont listes, non deduits par exclusion. L exclusion
     * ferait que chaque statut ajoute a StatutsActivite s ouvre par defaut,
     * alors qu un statut nouveau est precisement un cas qu on n a pas encore
     * tranche. La liste oblige a le dire.
     *
     * Un statut inconnu, null compris -- donc une activite sans historique --
     * n est pas dans la liste : il est refuse.
     *
     * Le meme controle existe cote formulaire, pour ne pas proposer une action
     * que le serveur refuserait. Celui-ci fait seul autorite, sous verrou.
     */
    private static boolean depotAutorise(String statutActivite) {
        return StatutsActivite.BROUILLON.equals(statutActivite)
                || StatutsActivite.EN_COURS.equals(statutActivite);
    }

    private void validerIdentifiants(Integer idActivite, Integer idSousActivite) {

        validerIdPositif(idActivite, "activite");
        validerIdPositif(idSousActivite, "sous-activite");
    }

    /**
     * L identifiant du livrable, valide a part.
     *
     * Il ne se trouve pas dans validerIdentifiants parce que seul le depot de
     * fichiers porte trois identifiants : la creation d un livrable n en a que
     * deux, et lui faire exiger un troisieme l obligerait a en produire un sans
     * usage.
     */
    private void validerIdLivrable(Integer idLivrable) {
        validerIdPositif(idLivrable, "livrable");
    }

    /**
     * Un identifiant absent ou nul, ou negatif, est refuse avant tout acces.
     *
     * Le message nomme l entite plutot que de dire "identifiant invalide" : a
     * cet endroit, l appelant a fourni une URL, et savoir lequel de ses
     * identifiants est en cause evite de les tester un par un. Un null n est
     * pas traite a part : il produirait de toute facon une erreur d acces en
     * base, seulement illisible.
     */
    private void validerIdPositif(Integer valeur, String quoi) {

        if (valeur == null || valeur < 1) {
            throw new ApiException(
                    "L identifiant de la " + quoi
                            + " doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }
    }

}
