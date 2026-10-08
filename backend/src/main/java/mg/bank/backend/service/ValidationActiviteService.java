package mg.bank.backend.service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.model.ValidationActivite;
import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.repository.ValidationActiviteRepository;
import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.model.Utilisateur;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValidationActiviteService {

    private final ValidationActiviteRepository validationActiviteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    public Optional<ValidationActivite> getDerniereValidationValidee(Integer idActivite) {
        return validationActiviteRepository
                .findFirstByActivite_IdActiviteAndDecisionOrderByDateDecisionDesc(
                        idActivite,
                        DecisionValidation.VALIDE);
    }

    public Optional<ValidationActivite> getDerniereValidation(Integer idActivite) {
        return validationActiviteRepository
                .findFirstByActivite_IdActiviteOrderByDateDecisionDesc(
                        idActivite);
    }

    @Transactional
    public ValidationActivite soumettre(ValidationActivite validationActivite) {
        boolean nouvelleEnAttente = validationActivite.getIdValidationActivite() == null
                && (validationActivite.getDecision() == null
                        || DecisionValidation.EN_ATTENTE_VALIDATION.equals(validationActivite.getDecision()));
        ValidationActivite sauvegardee = validationActiviteRepository.save(validationActivite);
        if (nouvelleEnAttente) {
            Integer idService = sauvegardee.getActivite().getService() == null
                    ? null
                    : sauvegardee.getActivite().getService().getIdService();
            if (idService != null) {
                utilisateurRepository.findDestinatairesEtape(
                        sauvegardee.getEtapeValidation().getIdEtapeValidation(), idService)
                        .forEach(destinataire -> emailService.sendValidationActiviteEmail(
                                destinataire, sauvegardee.getActivite()));
            }
        }
        return sauvegardee;
    }

    public Optional<ValidationActivite> getDerniereEtapeNonValidee(Integer idActivite) {
        return validationActiviteRepository.findDerniereEtapeNonValidee(idActivite);
    }

    public ValidationActivite valider(ValidationActivite validationActivite) {
        validationActivite.setDecision(DecisionValidation.VALIDE);
        return validationActiviteRepository.save(validationActivite);
    }

    @Transactional(readOnly = true)
    public Map<Integer, ValidationActivite> getDecisionsPrecedentesParActivite(Collection<Integer> idsActivites) {
        if (idsActivites == null || idsActivites.isEmpty()) {
            return Map.of();
        }
        return validationActiviteRepository
                .findDecisionsPrecedentesByActiviteIds(idsActivites, DecisionValidation.EN_ATTENTE_VALIDATION)
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getActivite().getIdActivite(),
                        Function.identity(),
                        (premier, second) -> premier // l'ORDER BY garantit que le 1er est le plus récent
                ));
    }
}
