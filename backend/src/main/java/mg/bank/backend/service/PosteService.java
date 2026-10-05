package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.PosteRequest;
import mg.bank.backend.dto.backoffice.PosteResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.PosteMapper;
import mg.bank.backend.model.Poste;
import mg.bank.backend.repository.PosteRepository;

@Service
@RequiredArgsConstructor
public class PosteService {

    private final PosteRepository posteRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<PosteResponse> lister(boolean inclureInactifs) {
        List<Poste> postes = inclureInactifs
                ? posteRepository.findAll()
                : posteRepository.findByActifTrue();

        return postes.stream().map(PosteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PosteResponse getById(Integer id) {
        return PosteMapper.toResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public PosteResponse creer(PosteRequest request) {
        Poste entity = Poste.builder()
                .libelle(request.getNom().trim())
                .effectifPrevu(request.getEffectifPrevu())
                .effectifReel(request.getEffectifReel())
                .isMetier(request.getIsMetier() == null ? Boolean.TRUE : request.getIsMetier())
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateModification(null)
                .dateDesactivation(null)
                .build();

        return PosteMapper.toResponse(posteRepository.save(entity));
    }

    @Transactional
    public PosteResponse modifier(Integer id, PosteRequest request) {
        Poste entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un poste désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setLibelle(request.getNom().trim());
        entity.setEffectifPrevu(request.getEffectifPrevu());
        entity.setEffectifReel(request.getEffectifReel());
        if (request.getIsMetier() != null) {
            entity.setIsMetier(request.getIsMetier());
        }
        entity.setDateModification(LocalDateTime.now());

        return PosteMapper.toResponse(posteRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public PosteResponse desactiver(Integer id) {
        Poste entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le poste est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());
        entity.setDateModification(LocalDateTime.now());

        return PosteMapper.toResponse(posteRepository.save(entity));
    }

    @Transactional
    public PosteResponse activer(Integer id) {
        Poste entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le poste est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);
        entity.setDateModification(LocalDateTime.now());

        return PosteMapper.toResponse(posteRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Poste getEntity(Integer id) {
        return posteRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Poste introuvable",
                        HttpStatus.NOT_FOUND));
    }
}