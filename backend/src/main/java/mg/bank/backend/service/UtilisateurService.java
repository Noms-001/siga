package mg.bank.backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        // 4. Construire l'utilisateur
        Utilisateur utilisateur = Utilisateur.builder()
                .departement(departement)
                .service(service)
                .poste(poste)
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
}
