package mg.bank.backend.service.referentiel;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.DepartementRequest;
import mg.bank.backend.dto.backoffice.DepartementResponse;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.referentiel.DepartementMapper;
import mg.bank.backend.model.referentiel.Departement;
import mg.bank.backend.repository.referentiel.DepartementRepository;
import mg.bank.backend.service.common.PorteeService;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final PorteeService porteeService;

    @Transactional(readOnly = true)
    public List<DepartementResponse> lister() {
        PerimetreUtilisateur perimetre = porteeService.perimetreCourant();
        return departementRepository.findAll()
                .stream()
                .filter(d -> porteeService.departementAccessible(
                        d.getIdDepartement(), perimetre))
                .map(DepartementMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartementResponse getById(Integer id) {
        return DepartementMapper.toResponse(getEntity(id));
    }

    @Transactional
    public DepartementResponse creer(DepartementRequest request) {
        if (!porteeService.porteeTous()) {
            throw new ApiException(
                    "La création d'un département nécessite une portée totale",
                    HttpStatus.FORBIDDEN);
        }

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
        if (!porteeService.porteeDepartementOuPlus()) {
            throw new ApiException(
                    "La modification d'un département nécessite une portée département ou totale",
                    HttpStatus.FORBIDDEN);
        }
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
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Département introuvable",
                        HttpStatus.NOT_FOUND));
        porteeService.exigerDepartement(id);
        return departement;
    }
}