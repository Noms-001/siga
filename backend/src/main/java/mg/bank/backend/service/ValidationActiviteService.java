package mg.bank.backend.service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.model.ValidationActivite;
import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.repository.ValidationActiviteRepository;
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

    public ValidationActivite soumettre(ValidationActivite validationActivite) {
        return validationActiviteRepository.save(validationActivite);
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
