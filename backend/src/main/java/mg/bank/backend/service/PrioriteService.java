package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.PrioriteResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.PrioriteMapper;
import mg.bank.backend.repository.PrioriteRepository;

@Service
@RequiredArgsConstructor
public class PrioriteService {

    private final PrioriteRepository prioriteRepository;

    @Transactional(readOnly = true)
    public List<PrioriteResponse> lister(boolean inclureInactifs) {
        return (inclureInactifs
                ? prioriteRepository.findAll()
                : prioriteRepository.findByActifTrue())
                .stream()
                .map(PrioriteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrioriteResponse getById(Integer id) {
        return PrioriteMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public mg.bank.backend.model.Priorite getEntity(Integer id) {
        return prioriteRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Priorité introuvable",
                        HttpStatus.NOT_FOUND));
    }
}