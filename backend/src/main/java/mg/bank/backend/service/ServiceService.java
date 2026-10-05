package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.ServiceRequest;
import mg.bank.backend.dto.backoffice.ServiceResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.ServiceMapper;
import mg.bank.backend.model.Departement;
import mg.bank.backend.repository.DepartementRepository;
import mg.bank.backend.repository.ServiceRepository;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final DepartementRepository departementRepository;

    @Transactional(readOnly = true)
    public List<ServiceResponse> lister(boolean inclureInactifs, Integer idDepartement) {
        List<mg.bank.backend.model.Service> services;
        if (idDepartement != null) {
            services = inclureInactifs
                    ? serviceRepository.findByDepartementIdDepartement(idDepartement)
                    : serviceRepository.findByDepartementIdDepartementAndActifTrue(idDepartement);
        } else {
            services = inclureInactifs
                    ? serviceRepository.findAll()
                    : serviceRepository.findByActifTrue();
        }
        return services.stream().map(ServiceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getById(Integer id) {
        return ServiceMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ServiceResponse creer(ServiceRequest request) {
        Departement departement = getDepartement(request.getIdDepartement());

        String nom = request.getNom().trim();
        if (serviceRepository.existsByNomAndDepartementIdDepartement(nom, departement.getIdDepartement())) {
            throw new ApiException(
                    "Un service avec ce nom existe déjà dans ce département",
                    HttpStatus.CONFLICT);
        }

        mg.bank.backend.model.Service entity = mg.bank.backend.model.Service.builder()
                .nom(nom)
                .description(request.getDescription())
                .departement(departement)
                .actif(true)
                .dateDesactivation(null)
                .build();

        return ServiceMapper.toResponse(serviceRepository.save(entity));
    }

    @Transactional
    public ServiceResponse modifier(Integer id, ServiceRequest request) {
        mg.bank.backend.model.Service entity = getEntity(id);
        Departement departement = getDepartement(request.getIdDepartement());

        String nom = request.getNom().trim();

        if (serviceRepository.existsByNomAndDepartementIdDepartementAndIdServiceNot(
                nom, departement.getIdDepartement(), id)) {
            throw new ApiException(
                    "Un service avec ce nom existe déjà dans ce département",
                    HttpStatus.CONFLICT);
        }

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un service désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setNom(nom);
        entity.setDescription(request.getDescription());
        entity.setDepartement(departement);

        return ServiceMapper.toResponse(serviceRepository.save(entity));
    }

    @Transactional
    public ServiceResponse desactiver(Integer id) {
        mg.bank.backend.model.Service entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le service est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());

        return ServiceMapper.toResponse(serviceRepository.save(entity));
    }

    @Transactional
    public ServiceResponse activer(Integer id) {
        mg.bank.backend.model.Service entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le service est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);

        return ServiceMapper.toResponse(serviceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public mg.bank.backend.model.Service getEntity(Integer id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Service introuvable",
                        HttpStatus.NOT_FOUND));
    }

    private Departement getDepartement(Integer idDepartement) {
        return departementRepository.findById(idDepartement)
                .orElseThrow(() -> new ApiException(
                        "Département introuvable",
                        HttpStatus.NOT_FOUND));
    }
}