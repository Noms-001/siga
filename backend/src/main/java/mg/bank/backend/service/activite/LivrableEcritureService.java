package mg.bank.backend.service.activite;

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
import mg.bank.backend.dto.activite.LivrableAjoutRequest;
import mg.bank.backend.dto.activite.LivrableDetailDTO;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Fichier;
import mg.bank.backend.model.activite.Livrable;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.FichierRepository;
import mg.bank.backend.repository.activite.LivrableRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;
import mg.bank.backend.repository.projection.activite.SousActiviteDetailRow;
import mg.bank.backend.service.utilisateur.CurrentUserService;
import mg.bank.backend.utils.FileNames;

@Service
@RequiredArgsConstructor
public class LivrableEcritureService {

    @Value("${app.depot.livrables:/data/saga/livrables}")
    private String racineDepot;

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
    private final CurrentUserService currentUserService;

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
                livrable, fichiers, currentUserService.get(), 0);

        return LivrableDetailDTO.builder()
                .id(livrable.getIdLivrable())
                .designation(livrable.getDesignation())
                .description(livrable.getDescription())
                .fichiers(activiteDetailService.fichiers(livrable.getIdLivrable()))
                .build();
    }

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

        Livrable livrable = livrableRepository
                .findByIdLivrableAndSousActiviteIdSousActivite(
                        idLivrable, idSousActivite)
                .orElseThrow(() -> new ApiException(
                        "Livrable introuvable pour cette sous-activité",
                        HttpStatus.NOT_FOUND));

        int dejaDeposes = (int) livrableRepository.compterFichiers(idLivrable);

        deposerFichiers(
                livrable, fichiers, currentUserService.get(), dejaDeposes);

        return LivrableDetailDTO.builder()
                .id(livrable.getIdLivrable())
                .designation(livrable.getDesignation())
                .description(livrable.getDescription())
                .fichiers(activiteDetailService.fichiers(idLivrable))
                .build();
    }

    private SousActiviteDetailRow exigerDepotAutorise(
            Integer idActivite,
            Integer idSousActivite,
            String messageStatutRefuse) {

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        activiteRepository.verrouillerActivite(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        SousActiviteDetailRow demandee = activiteRepository.findSousActivite(
                idActivite,
                idSousActivite,
                perimetre.getFiltreService(),
                perimetre.getFiltreDepartement())
                .orElseThrow(() -> activiteDetailService
                        .refusOuAbsenceSousActivite(idActivite, idSousActivite));

        if (!depotAutorise(demandee.getStatutActivite())) {
            throw new ApiException(messageStatutRefuse, HttpStatus.CONFLICT);
        }

        return demandee;
    }

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
                        .nomOriginal(FileNames.nomOriginalSecurise(fichier.getOriginalFilename()))
                        .cheminFichier(ecrit.toAbsolutePath().toString())
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

            fichierRepository.flush();
        } catch (IOException e) {
            // Le rollback annule les lignes, pas les octets deja ecrits.
            supprimer(ecrits);
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

    private Path ecrireUn(MultipartFile fichier, Livrable livrable, int rang)
            throws IOException {

        Path dossier = Path.of(racineDepot)
                .resolve(String.valueOf(livrable.getIdLivrable()));

        Files.createDirectories(dossier);

        String nomStockage = FileNames.nomDeStockage(
                livrable.getIdLivrable(), 1, rang, FileNames.extension(fichier.getOriginalFilename()));
        Path cible = dossier.resolve(nomStockage);

        try (InputStream entree = fichier.getInputStream()) {
            Files.copy(entree, cible, StandardCopyOption.REPLACE_EXISTING);
        }

        return cible;
    }

    private void validerFichier(MultipartFile fichier) {

        if (fichier.getSize() > tailleMax) {
            throw new ApiException(
                    "Un fichier dépasse la taille maximale autorisée",
                    HttpStatus.BAD_REQUEST);
        }
    }

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

            }
        }
    }

    private static boolean depotAutorise(String statutActivite) {
        return StatutsActivite.BROUILLON.equals(statutActivite)
                || StatutsActivite.EN_COURS.equals(statutActivite);
    }

    private void validerIdentifiants(Integer idActivite, Integer idSousActivite) {

        validerIdPositif(idActivite, "activite");
        validerIdPositif(idSousActivite, "sous-activite");
    }

    private void validerIdLivrable(Integer idLivrable) {
        validerIdPositif(idLivrable, "livrable");
    }

    private void validerIdPositif(Integer valeur, String quoi) {

        if (valeur == null || valeur < 1) {
            throw new ApiException(
                    "L identifiant de la " + quoi
                            + " doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }
    }

}
