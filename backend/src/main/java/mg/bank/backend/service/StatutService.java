package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.StatutResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.StatutMapper;
import mg.bank.backend.repository.StatutRepository;

@Service
@RequiredArgsConstructor
public class StatutService {

    private final StatutRepository statutRepository;

    @Transactional(readOnly = true)
    public List<StatutResponse> lister(boolean inclureInactifs) {
        return (inclureInactifs
                ? statutRepository.findAll()
                : statutRepository.findByActifTrue())
                .stream()
                .map(StatutMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutResponse getById(Integer id) {
        return StatutMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public mg.bank.backend.model.Statut getEntity(Integer id) {
        return statutRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Statut introuvable",
                        HttpStatus.NOT_FOUND));
    }
}