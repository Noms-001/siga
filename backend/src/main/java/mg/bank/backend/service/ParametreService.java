package mg.bank.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.ParametreFiltresResponse;
import mg.bank.backend.dto.backoffice.ParametreResponse;
import mg.bank.backend.dto.backoffice.ParametreUpdateRequest;
import mg.bank.backend.enums.ParametreEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.ParametreMapper;
import mg.bank.backend.model.Parametre;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ParametreRepository;
import mg.bank.backend.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class ParametreService {

    private final ParametreRepository parametreRepository;
    private final UtilisateurRepository utilisateurRepository;

    public Parametre getParametre(ParametreEnum parametreEnum) {

        return parametreRepository
                .findByCodeAndActifTrue(parametreEnum.getCode())
                .orElseThrow(() ->
                        new ApiException(
                                "Paramètre introuvable",
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );
    }

    public long getLongValue(ParametreEnum parametreEnum) {

        Parametre parametre = getParametre(parametreEnum);

        try {
            return Long.parseLong(parametre.getValeur());
        } catch (NumberFormatException e) {
            throw new ApiException(
                    "Valeur du paramètre invalide",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<ParametreResponse> lister(Boolean actif, String categorie) {
        String cat = (categorie == null || categorie.isBlank()) ? null : categorie;
        return parametreRepository.rechercher(actif, cat)
                .stream()
                .map(ParametreMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParametreResponse getById(Integer id) {
        return ParametreMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public ParametreFiltresResponse getFiltres() {
        return ParametreFiltresResponse.builder()
                .categories(parametreRepository.findDistinctCategories())
                .build();
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public ParametreResponse modifier(Integer id, ParametreUpdateRequest request) {
        Parametre entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un paramètre désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        if (Boolean.FALSE.equals(entity.getModifiable())) {
            throw new ApiException(
                    "Ce paramètre n'est pas modifiable",
                    HttpStatus.BAD_REQUEST);
        }

        String valeurValidee = validerValeur(entity.getTypeValeur(), request.getValeur());

        entity.setValeur(valeurValidee);
        entity.setDateModification(LocalDateTime.now());
        entity.setUtilisateurModification(getUtilisateurCourant());

        return ParametreMapper.toResponse(parametreRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public ParametreResponse desactiver(Integer id) {
        Parametre entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le paramètre est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());

        return ParametreMapper.toResponse(parametreRepository.save(entity));
    }

    @Transactional
    public ParametreResponse activer(Integer id) {
        Parametre entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le paramètre est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);

        return ParametreMapper.toResponse(parametreRepository.save(entity));
    }

    /* -------------------- validation de la valeur -------------------- */

    /**
     * Vérifie que la valeur fournie respecte le type déclaré du paramètre, et
     * la renvoie sous une forme normalisée (booléens en minuscules, par
     * exemple).
     *
     * Le schéma PostgreSQL contraint `type_valeur` à un jeu fixe ; le `default`
     * ci-dessous ne peut donc être atteint qu'en cas de donnée corrompue en
     * base — on préfère une exception explicite à un cast silencieux.
     */
    private String validerValeur(String typeValeur, String valeurBrute) {
        if (valeurBrute == null) {
            throw new ApiException("La valeur est obligatoire", HttpStatus.BAD_REQUEST);
        }

        String v = valeurBrute.trim();

        switch (typeValeur) {
            case "STRING":
                if (v.isEmpty()) {
                    throw new ApiException("La valeur ne peut pas être vide", HttpStatus.BAD_REQUEST);
                }
                return v;

            case "INTEGER":
                try {
                    Long.parseLong(v);
                } catch (NumberFormatException e) {
                    throw new ApiException(
                            "La valeur doit être un entier",
                            HttpStatus.BAD_REQUEST);
                }
                return v;

            case "DECIMAL":
                try {
                    new BigDecimal(v);
                } catch (NumberFormatException e) {
                    throw new ApiException(
                            "La valeur doit être un nombre décimal",
                            HttpStatus.BAD_REQUEST);
                }
                return v;

            case "BOOLEAN":
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    throw new ApiException(
                            "La valeur doit être true ou false",
                            HttpStatus.BAD_REQUEST);
                }
                return v.toLowerCase();

            case "DATE":
                try {
                    LocalDate.parse(v);
                } catch (Exception e) {
                    throw new ApiException(
                            "La valeur doit être une date au format AAAA-MM-JJ",
                            HttpStatus.BAD_REQUEST);
                }
                return v;

            case "DATETIME":
                try {
                    LocalDateTime.parse(v);
                } catch (Exception e) {
                    throw new ApiException(
                            "La valeur doit être une date-heure au format AAAA-MM-JJTHH:MM:SS",
                            HttpStatus.BAD_REQUEST);
                }
                return v;

            default:
                throw new ApiException(
                        "Type de valeur inconnu : " + typeValeur,
                        HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Parametre getEntity(Integer id) {
        return parametreRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Paramètre introuvable",
                        HttpStatus.NOT_FOUND));
    }

    /**
     * Retrouve l'utilisateur authentifié à partir du contexte de sécurité.
     *
     * L'identifiant de connexion est l'email — c'est ce que
     * `UtilisateurService.getUtilisateurByEmail` consomme. Un contexte
     * anonyme ou un email inconnu renvoie null plutôt que de faire échouer
     * la modification : la colonne `id_utilisateur_modification` est
     * nullable, et perdre une mise à jour de paramètre pour un problème
     * d'audit serait disproportionné.
     */
    private Utilisateur getUtilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        return utilisateurRepository.findByEmail(auth.getName()).orElse(null);
    }
}