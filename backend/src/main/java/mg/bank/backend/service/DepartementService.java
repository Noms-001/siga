package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.DepartementRequest;
import mg.bank.backend.dto.backoffice.DepartementResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.DepartementMapper;
import mg.bank.backend.model.Departement;
import mg.bank.backend.repository.DepartementRepository;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;

    @Transactional(readOnly = true)
    public List<DepartementResponse> lister() {
        return departementRepository.findAll()
                .stream()
                .map(DepartementMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartementResponse getById(Integer id) {
        return DepartementMapper.toResponse(getEntity(id));
    }

    @Transactional
    public DepartementResponse creer(DepartementRequest request) {
        String code = request.getCode().trim();

        if (departementRepository.existsByCode(code)) {
            throw new ApiException(
                    "Un département avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        Departement entity = DepartementMapper.toEntity(request);
        return DepartementMapper.toResponse(departementRepository.save(entity));
    }

    @Transactional
    public DepartementResponse modifier(Integer id, DepartementRequest request) {
        Departement entity = getEntity(id);

        String code = request.getCode().trim();

        if (departementRepository.existsByCodeAndIdDepartementNot(code, id)) {
            throw new ApiException(
                    "Un département avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        entity.setCode(code);
        entity.setNom(request.getNom().trim());
        entity.setDescription(request.getDescription());

        return DepartementMapper.toResponse(departementRepository.save(entity));
    }

    /** Exposé pour la Phase suivante (TypeActivite, etc.). */
    @Transactional(readOnly = true)
    public Departement getEntity(Integer id) {
        return departementRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Département introuvable",
                        HttpStatus.NOT_FOUND));
    }
}