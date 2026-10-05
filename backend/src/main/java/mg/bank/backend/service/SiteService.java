package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.SiteRequest;
import mg.bank.backend.dto.backoffice.SiteResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.SiteMapper;
import mg.bank.backend.model.Site;
import mg.bank.backend.repository.SiteRepository;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<SiteResponse> lister(boolean inclureInactifs) {
        List<Site> sites = inclureInactifs
                ? siteRepository.findAll()
                : siteRepository.findByActifTrue();
        return sites.stream().map(SiteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SiteResponse getById(Integer id) {
        return SiteMapper.toResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public SiteResponse creer(SiteRequest request) {
        String nom = request.getNom().trim();

        if (siteRepository.existsByNom(nom)) {
            throw new ApiException(
                    "Un site avec ce nom existe déjà",
                    HttpStatus.CONFLICT);
        }

        Site entity = Site.builder()
                .nom(nom)
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateDesactivation(null)
                .build();

        return SiteMapper.toResponse(siteRepository.save(entity));
    }

    @Transactional
    public SiteResponse modifier(Integer id, SiteRequest request) {
        Site entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un site désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        String nom = request.getNom().trim();

        if (siteRepository.existsByNomAndIdSiteNot(nom, id)) {
            throw new ApiException(
                    "Un site avec ce nom existe déjà",
                    HttpStatus.CONFLICT);
        }

        entity.setNom(nom);

        return SiteMapper.toResponse(siteRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public SiteResponse desactiver(Integer id) {
        Site entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le site est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());

        return SiteMapper.toResponse(siteRepository.save(entity));
    }

    @Transactional
    public SiteResponse activer(Integer id) {
        Site entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le site est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);

        return SiteMapper.toResponse(siteRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Site getEntity(Integer id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Site introuvable",
                        HttpStatus.NOT_FOUND));
    }
}