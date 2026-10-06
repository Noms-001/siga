package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ModifierProfilRequest;
import mg.bank.backend.dto.UtilisateurRequest;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Departement;
import mg.bank.backend.model.Poste;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;

import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.repository.DepartementRepository;
import mg.bank.backend.repository.PosteRepository;
import mg.bank.backend.repository.ServiceRepository;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;
    private final ServiceRepository serviceRepository;
    private final PosteRepository posteRepository;
    private final TokenAuthService tokenAuthService;
    private final EmailService emailService;

    public Utilisateur saveUtilisateur(UtilisateurRequest request) {

        // 1. Vérifier le département
        Departement departement = departementRepository.findById(request.getIdDepartement())
                .orElseThrow(() -> new ApiException(
                "Département introuvable",
                HttpStatus.NOT_FOUND));

        // 2. Vérifier le service s'il est fourni
        mg.bank.backend.model.Service service = null;

        if (request.getIdService() != null) {

            service = serviceRepository.findById(request.getIdService())
                    .orElseThrow(() -> new ApiException(
                    "Service introuvable",
                    HttpStatus.NOT_FOUND));

            // Vérifier que le service appartient au département sélectionné
            if (!service.getDepartement().getIdDepartement()
                    .equals(departement.getIdDepartement())) {

                throw new ApiException(
                        "Le service n'appartient pas au département sélectionné",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // 3. Vérifier le poste
        Poste poste = posteRepository.findById(request.getIdPoste())
                .orElseThrow(() -> new ApiException(
                "Poste introuvable",
                HttpStatus.NOT_FOUND));

        Set<Poste> postes = new HashSet<>();
        postes.add(poste);

        // 4. Construire l'utilisateur
        Utilisateur utilisateur = Utilisateur.builder()
                .departement(departement)
                .service(service)
                .postes(postes)
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .motDePasse(null)
                .actif(false)
                .dateCreation(LocalDateTime.now())
                .dateDesactivation(null)
                .dateDerniereConnexion(null)
                .build();

        // 5. Enregistrer
        return utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public Utilisateur createUtilisateur(UtilisateurRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (utilisateurRepository.existsByEmail(email)) {
            throw new ApiException(
                    "Un utilisateur avec cet email existe déjà",
                    HttpStatus.CONFLICT);
        }

        request.setEmail(email);

        Utilisateur utilisateur = saveUtilisateur(request);

        TokenAuth tokenAuth = tokenAuthService.createActivationToken(utilisateur);

        emailService.sendActivationEmail(
                utilisateur,
                tokenAuth);

        return utilisateur;
    }

    public Utilisateur getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                "Email ou mot de passe incorrect",
                HttpStatus.UNAUTHORIZED));
    }

    public Utilisateur updateUtilisateur(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Met à jour les informations personnelles de l'utilisateur connecté.
     *
     * L'email est la clé de connexion et de réinitialisation de mot de passe.
     * La table utilisateur ne porte aucune contrainte UNIQUE sur cette colonne,
     * le contrôle du doublon est donc fait ici, à niveau applicatif.
     *
     * @return l'utilisateur mis à jour
     */
    @Transactional
    public Utilisateur modifierProfil(
            String emailActuel,
            ModifierProfilRequest request) {

        Utilisateur utilisateur = getUtilisateurByEmail(emailActuel);

        String nouvelEmail = request.getEmail().trim().toLowerCase();

        boolean emailModifie = !nouvelEmail.equalsIgnoreCase(utilisateur.getEmail());

        if (emailModifie) {
            boolean dejaUtilise = utilisateurRepository
                    .findByEmail(nouvelEmail)
                    .filter(other -> !other.getIdUtilisateur().equals(utilisateur.getIdUtilisateur()))
                    .isPresent();

            if (dejaUtilise) {
                throw new ApiException(
                        "Cet email est déjà utilisé par un autre compte",
                        HttpStatus.CONFLICT);
            }
        }

        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(nouvelEmail);
        utilisateur.setTelephone(
                request.getTelephone() == null || request.getTelephone().isBlank()
                        ? null
                        : request.getTelephone().trim());
        utilisateur.setDateModification(LocalDateTime.now());

        return utilisateurRepository.save(utilisateur);
    }

    /* -------------------- lecture Backoffice -------------------- */

        @Transactional(readOnly = true)
        public List<mg.bank.backend.dto.backoffice.UtilisateurResponse> lister(
                Boolean actif,
                Integer idDepartement,
                Integer idService) {

        return utilisateurRepository.rechercher(actif, idDepartement, idService)
                .stream()
                .map(mg.bank.backend.mapper.UtilisateurMapper::toResponse)
                .toList();
        }

        @Transactional(readOnly = true)
        public mg.bank.backend.dto.backoffice.UtilisateurResponse getById(Integer id) {
        Utilisateur u = utilisateurRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable",
                        HttpStatus.NOT_FOUND));
        return mg.bank.backend.mapper.UtilisateurMapper.toResponse(u);
        }

        /* -------------------- désactivation / activation -------------------- */

        /**
         * Désactive un utilisateur (logique — aucune suppression physique).
         *
         * L'utilisateur désactivé ne pourra plus s'authentifier si le filtre de
         * sécurité vérifie `actif`. Ce point doit être confirmé côté SecurityConfig :
         * la désactivation seule ne suffit pas si le filtre n'inspecte pas ce
         * drapeau.
         */
        @Transactional
        public mg.bank.backend.dto.backoffice.UtilisateurResponse desactiverUtilisateur(Integer id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable",
                        HttpStatus.NOT_FOUND));

        if (Boolean.FALSE.equals(u.getActif())) {
                throw new ApiException(
                        "L'utilisateur est déjà désactivé",
                        HttpStatus.BAD_REQUEST);
        }

        u.setActif(false);
        u.setDateDesactivation(LocalDateTime.now());
        u.setDateModification(LocalDateTime.now());

        return mg.bank.backend.mapper.UtilisateurMapper.toResponse(
                utilisateurRepository.save(u));
        }

        @Transactional
        public mg.bank.backend.dto.backoffice.UtilisateurResponse activerUtilisateur(Integer id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable",
                        HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(u.getActif())) {
                throw new ApiException(
                        "L'utilisateur est déjà actif",
                        HttpStatus.BAD_REQUEST);
        }

        if (u.getMotDePasse() == null) {
                throw new ApiException(
                        "Cet utilisateur n'a pas encore activé son compte. "
                                + "Il doit utiliser le lien d'activation reçu par email.",
                        HttpStatus.BAD_REQUEST);
        }

        u.setActif(true);
        u.setDateDesactivation(null);
        u.setDateModification(LocalDateTime.now());

        return mg.bank.backend.mapper.UtilisateurMapper.toResponse(
                utilisateurRepository.save(u));
        }
}
