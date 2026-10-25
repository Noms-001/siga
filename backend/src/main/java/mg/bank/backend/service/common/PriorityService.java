package mg.bank.backend.service.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.repository.referentiel.PrioriteRepository;

@Service
@RequiredArgsConstructor
public class PriorityService {

    private final PrioriteRepository prioriteRepository;

    @Transactional(readOnly = true)
    public Priorite getForNotification() {
        return prioriteRepository.findByCodeIgnoreCase("HAUTE")
                .filter(Priorite::getActif)
                .orElseGet(() -> prioriteRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActif()))
                        .findFirst()
                        .orElse(null));
    }
}