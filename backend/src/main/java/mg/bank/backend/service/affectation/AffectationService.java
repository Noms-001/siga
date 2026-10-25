package mg.bank.backend.service.affectation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteListItemDTO;
import mg.bank.backend.dto.affectation.AffectationActiviteDTO;
import mg.bank.backend.dto.affectation.AffectationActiviteRequest;
import mg.bank.backend.dto.affectation.UtilisateurAffectableDTO;
import mg.bank.backend.dto.common.ActiviteFiltreCriteria;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.activite.ActiviteMapper;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.model.affectation.AffectationActivite;
import mg.bank.backend.model.affectation.AffectationSousActivite;
import mg.bank.backend.model.utilisateur.Role;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationSousActiviteRepository;
import mg.bank.backend.repository.projection.activite.ActiviteListRow;
import mg.bank.backend.repository.projection.affectation.AffectationRow;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.repository.utilisateur.RoleRepository;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.utilisateur.CurrentUserService;
import mg.bank.backend.utils.ReferenceMapper;

@Service
@RequiredArgsConstructor
public class AffectationService {

        /** Roles autorises pour une affectation. */
        private static final Set<String> ROLES_VALIDES = Set.of("RESPONSABLE", "SUPPLEANT", "PARTICIPANT");

        /** Roles dont l'affectation est unique (un seul actif par entite). */
        private static final Set<String> ROLES_SINGULIERS = Set.of("RESPONSABLE", "SUPPLEANT");

        private static final String CODE_SUPPLEANT = "SUPPLEANT";
        private static final String STATUT_NON_COMMENCEE = "NON_COMMENCEE";
        private static final int TAILLE_PAGE_MAX = 100;

        private final ActiviteRepository activiteRepository;
        private final SousActiviteRepository sousActiviteRepository;
        private final AffectationActiviteRepository affectationActiviteRepository;
        private final AffectationSousActiviteRepository affectationSousActiviteRepository;
        private final UtilisateurRepository utilisateurRepository;
        private final RoleRepository roleRepository;

        private final ActiviteService activiteService;

        private final CurrentUserService currentUserService;

        // ------------------------------------------------------------------
        // F1 — Liste des activites non affectees
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public PageResponse<ActiviteListItemDTO> rechercherNonAffectees(
                        ActiviteFiltreCriteria filtres,
                        int page,
                        int size,
                        Sort sort) {

                if (page < 0) {
                        throw new ApiException("Le numéro de page ne peut pas être négatif",
                                        HttpStatus.BAD_REQUEST);
                }
                if (size < 1 || size > TAILLE_PAGE_MAX) {
                        throw new ApiException(
                                        "La taille de page doit être comprise entre 1 et " + TAILLE_PAGE_MAX,
                                        HttpStatus.BAD_REQUEST);
                }

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                Sort tri = (sort == null || sort.isUnsorted())
                                ? Sort.by(Sort.Direction.DESC, "dateCreationTri")
                                : sort;

                Pageable pageable = PageRequest.of(page, size, tri);

                Boolean pta = (filtres == null || filtres.getPta() == null) ? Boolean.TRUE : filtres.getPta();
                boolean horsPta = Boolean.FALSE.equals(pta);
                Integer servicePerimetre = perimetre.getFiltreService();
                Integer departementPerimetre = perimetre.getFiltreDepartement();
                Integer serviceDemande = filtres == null ? null : filtres.getServiceId();

                Page<ActiviteListItemDTO> resultat = affectationActiviteRepository
                                .findActivitesNonAffectees(
                                                pta,
                                                motif(filtres == null ? null : filtres.getSearch()),
                                                horsPta ? null
                                                                : (filtres == null ? null
                                                                                : filtres.getObjectifSpecifiqueId()),
                                                horsPta ? null
                                                                : motif(filtres == null ? null
                                                                                : filtres.getObjectifSearch()),
                                                servicePerimetre,
                                                departementPerimetre,
                                                serviceDemande,
                                                filtres == null ? null : filtres.getPrioriteId(),
                                                filtres == null ? null : filtres.getTypeActiviteId(),
                                                filtres == null ? null : filtres.getSiteId(),
                                                null,
                                                codesTerminaux(),
                                                filtres == null ? null : filtres.getAnnee(),
                                                moisDebut(filtres == null ? null : filtres.getTrimestre()),
                                                moisFin(filtres == null ? null : filtres.getTrimestre()),
                                                filtres == null ? null : filtres.getDateDebut(),
                                                filtres == null ? null : filtres.getDateFin(),
                                                pageable)
                                .map(ActiviteMapper::versDto);

                return PageResponse.from(resultat);
        }

        // ------------------------------------------------------------------
        // F2 / F3 — Historique des affectations
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public List<AffectationActiviteDTO> listerAffectations(Integer idActivite) {
                chargerActiviteDansPerimetre(idActivite);
                return affectationActiviteRepository.findHistorique(idActivite)
                                .stream().map(this::versAffectationDto).toList();
        }

        @Transactional(readOnly = true)
        public List<AffectationActiviteDTO> listerAffectationsSousActivite(
                        Integer idActivite, Integer idSousActivite) {
                chargerSousActiviteDansPerimetre(idActivite, idSousActivite);
                return affectationSousActiviteRepository.findHistorique(idSousActivite)
                                .stream().map(this::versAffectationDto).toList();
        }

        // ------------------------------------------------------------------
        // F4 — Utilisateurs affectables
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public List<UtilisateurAffectableDTO> utilisateursAffectables(Integer idActivite) {
                Activite activite = chargerActiviteDansPerimetre(idActivite);
                verifierStatutNonCommencee(idActivite);

                if (activite.getService() == null) {
                        throw new ApiException(
                                        "L'activité n'est rattachée à aucun service : impossible de "
                                                        + "déterminer les utilisateurs affectables.",
                                        HttpStatus.CONFLICT);
                }

                return utilisateurRepository
                                .findActifsParService(activite.getService().getIdService())
                                .stream()
                                .map(u -> UtilisateurAffectableDTO.builder()
                                                .id(u.getIdUtilisateur())
                                                .nom(u.getNom())
                                                .prenom(u.getPrenom())
                                                .build())
                                .toList();
        }

        // ------------------------------------------------------------------
        // F5 — Affecter une activite
        // ------------------------------------------------------------------

        @Transactional
        public AffectationActiviteDTO affecterActivite(
                        Integer idActivite, AffectationActiviteRequest request) {

                Activite activite = chargerActiviteDansPerimetre(idActivite);
                verifierStatutNonCommencee(idActivite);

                Utilisateur utilisateur = verifierUtilisateurAffectable(
                                request.getUtilisateurId(), activite);
                Role role = verifierRole(request.getRoleId());

                // Unicite pour les roles singuliers (le trigger en base fait la meme
                // verification : c'est la defense en profondeur — message propre ici,
                // integrite garantie par la base).
                if (ROLES_SINGULIERS.contains(role.getCode())
                                && affectationActiviteRepository
                                                .countActivesPourRole(idActivite, role.getIdRole()) > 0) {
                        throw new ApiException(
                                        "Un « " + role.getLibelle() + " » est déjà affecté à cette activité.",
                                        HttpStatus.CONFLICT);
                }

                // Regle de coherence : un suppleant exige un responsable actif.
                if (CODE_SUPPLEANT.equals(role.getCode())
                                && affectationActiviteRepository.countResponsableActif(idActivite) == 0) {
                        throw new ApiException(
                                        "Un suppléant ne peut être affecté que si un responsable est "
                                                        + "déjà affecté sur cette activité.",
                                        HttpStatus.CONFLICT);
                }

                AffectationActivite entity = AffectationActivite.builder()
                                .dateAffectation(LocalDateTime.now())
                                .role(role)
                                .activite(activite)
                                .utilisateur(utilisateur)
                                .build();

                try {
                        return versAffectationDto(affectationActiviteRepository.save(entity));
                } catch (DataIntegrityViolationException e) {
                        // Filet de securite : le trigger a joue (course entre deux
                        // requetes simultanees qui passent toutes deux le check service
                        // avant que l'une n'ait insere).
                        throw new ApiException(
                                        "Un « " + role.getLibelle()
                                                        + " » est déjà affecté à cette activité (conflit concurrent).",
                                        HttpStatus.CONFLICT);
                }
        }

        // ------------------------------------------------------------------
        // F6 — Affecter une sous-activite
        // ------------------------------------------------------------------

        @Transactional
        public AffectationActiviteDTO affecterSousActivite(
                        Integer idActivite, Integer idSousActivite,
                        AffectationActiviteRequest request) {

                SousActivite sa = chargerSousActiviteDansPerimetre(idActivite, idSousActivite);
                verifierStatutNonCommencee(idActivite);

                Utilisateur utilisateur = verifierUtilisateurAffectable(
                                request.getUtilisateurId(), sa.getActivite());
                Role role = verifierRole(request.getRoleId());

                if (ROLES_SINGULIERS.contains(role.getCode())
                                && affectationSousActiviteRepository
                                                .countActivesPourRole(idSousActivite, role.getIdRole()) > 0) {
                        throw new ApiException(
                                        "Un « " + role.getLibelle()
                                                        + " » est déjà affecté à cette sous-activité.",
                                        HttpStatus.CONFLICT);
                }

                if (CODE_SUPPLEANT.equals(role.getCode())
                                && affectationSousActiviteRepository.countResponsableActif(idSousActivite) == 0) {
                        throw new ApiException(
                                        "Un suppléant ne peut être affecté que si un responsable est "
                                                        + "déjà affecté sur cette sous-activité.",
                                        HttpStatus.CONFLICT);
                }

                AffectationSousActivite entity = AffectationSousActivite.builder()
                                .dateAffectation(LocalDateTime.now())
                                .role(role)
                                .sousActivite(sa)
                                .utilisateur(utilisateur)
                                .build();

                try {
                        return versAffectationSousActiviteDto(
                                        affectationSousActiviteRepository.save(entity));
                } catch (DataIntegrityViolationException e) {
                        throw new ApiException(
                                        "Un « " + role.getLibelle()
                                                        + " » est déjà affecté à cette sous-activité (conflit concurrent).",
                                        HttpStatus.CONFLICT);
                }
        }

        // ------------------------------------------------------------------
        // F7 / F8 — Desaffectation
        // ------------------------------------------------------------------

        @Transactional
        public void desaffecterActivite(Integer idActivite, Integer idAffectation) {
                chargerActiviteDansPerimetre(idActivite);
                verifierStatutNonCommencee(idActivite);

                AffectationActivite entity = affectationActiviteRepository
                                .findByIdAffectationActiviteAndActiviteIdActivite(idAffectation, idActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Affectation introuvable pour cette activité.",
                                                HttpStatus.NOT_FOUND));

                if (entity.getDateDesaffectation() != null) {
                        throw new ApiException("Cette affectation est déjà désactivée.",
                                        HttpStatus.CONFLICT);
                }

                entity.setDateDesaffectation(LocalDateTime.now());
                affectationActiviteRepository.save(entity);
        }

        @Transactional
        public void desaffecterSousActivite(
                        Integer idActivite, Integer idSousActivite, Integer idAffectation) {

                chargerSousActiviteDansPerimetre(idActivite, idSousActivite);
                verifierStatutNonCommencee(idActivite);

                AffectationSousActivite entity = affectationSousActiviteRepository
                                .findByIdAffectationSousActiviteAndSousActiviteIdSousActivite(
                                                idAffectation, idSousActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Affectation introuvable pour cette sous-activité.",
                                                HttpStatus.NOT_FOUND));

                if (entity.getDateDesaffectation() != null) {
                        throw new ApiException("Cette affectation est déjà désactivée.",
                                        HttpStatus.CONFLICT);
                }

                entity.setDateDesaffectation(LocalDateTime.now());
                affectationSousActiviteRepository.save(entity);
        }

        // ------------------------------------------------------------------
        // Helpers — chargement et controles
        // ------------------------------------------------------------------

        private Activite chargerActiviteDansPerimetre(Integer idActivite) {
                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                Integer servicePerimetre = perimetre.getFiltreService();
                Integer departementPerimetre = perimetre.getFiltreDepartement();

                if (activiteRepository.findActiviteDetail(
                                idActivite, servicePerimetre, departementPerimetre, codesTerminaux()).isEmpty()) {
                        throw activiteService.refusOuAbsence(idActivite);
                }
                return activiteRepository.findById(idActivite)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));
        }

        private SousActivite chargerSousActiviteDansPerimetre(Integer idActivite, Integer idSousActivite) {
                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                Integer servicePerimetre = perimetre.getFiltreService();
                Integer departementPerimetre = perimetre.getFiltreDepartement();

                if (activiteRepository.findSousActivite(
                                idActivite, idSousActivite, servicePerimetre, departementPerimetre).isEmpty()) {
                        throw activiteService.refusOuAbsence(idActivite);
                }
                return sousActiviteRepository.findById(idSousActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Sous-activité introuvable.", HttpStatus.NOT_FOUND));
        }

        private void verifierStatutNonCommencee(Integer idActivite) {
                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                StatutCourantRow statut = activiteRepository.findStatutCourant(
                                idActivite,
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement())
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                if (!STATUT_NON_COMMENCEE.equals(statut.getStatutCode())) {
                        throw new ApiException(
                                        "Seule une activité au statut « Non commencée » peut être affectée.",
                                        HttpStatus.CONFLICT);
                }
        }

        private Utilisateur verifierUtilisateurAffectable(Integer idUtilisateur, Activite activite) {
                Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                                .orElseThrow(() -> new ApiException(
                                                "Utilisateur introuvable.", HttpStatus.BAD_REQUEST));

                if (!Boolean.TRUE.equals(utilisateur.getActif())) {
                        throw new ApiException(
                                        "L'utilisateur sélectionné n'est pas actif.",
                                        HttpStatus.BAD_REQUEST);
                }
                if (utilisateur.getService() == null || activite.getService() == null
                                || !utilisateur.getService().getIdService()
                                                .equals(activite.getService().getIdService())) {
                        throw new ApiException(
                                        "L'utilisateur n'appartient pas au service de l'activité.",
                                        HttpStatus.BAD_REQUEST);
                }
                return utilisateur;
        }

        private Role verifierRole(Integer idRole) {
                Role role = roleRepository.findById(idRole)
                                .orElseThrow(() -> new ApiException(
                                                "Rôle introuvable.", HttpStatus.BAD_REQUEST));

                if (!Boolean.TRUE.equals(role.getActif())) {
                        throw new ApiException(
                                        "Le rôle sélectionné n'est pas actif.",
                                        HttpStatus.BAD_REQUEST);
                }
                if (!ROLES_VALIDES.contains(role.getCode())) {
                        throw new ApiException(
                                        "Le rôle « " + role.getLibelle()
                                                        + " » n'est pas autorisé pour une affectation.",
                                        HttpStatus.BAD_REQUEST);
                }
                return role;
        }

        // ------------------------------------------------------------------
        // Helpers — mapping
        // ------------------------------------------------------------------

        private AffectationActiviteDTO versAffectationDto(AffectationActivite a) {
                return AffectationActiviteDTO.builder()
                                .id(a.getIdAffectationActivite())
                                .dateAffectation(a.getDateAffectation())
                                .dateDesaffectation(a.getDateDesaffectation())
                                .role(ReferenceMapper.fromRole(a.getRole()))
                                .utilisateur(ReferenceMapper.fromUtilisateur(a.getUtilisateur()))
                                .active(a.getDateDesaffectation() == null)
                                .build();
        }

        private AffectationActiviteDTO versAffectationSousActiviteDto(AffectationSousActivite a) {
                return AffectationActiviteDTO.builder()
                                .id(a.getIdAffectationSousActivite())
                                .dateAffectation(a.getDateAffectation())
                                .dateDesaffectation(a.getDateDesaffectation())
                                .role(ReferenceMapper.fromRole(a.getRole()))
                                .utilisateur(ReferenceMapper.fromUtilisateur(a.getUtilisateur()))
                                .active(a.getDateDesaffectation() == null)
                                .build();
        }

        private AffectationActiviteDTO versAffectationDto(AffectationRow row) {
                return AffectationActiviteDTO.builder()
                                .id(row.getId())
                                .dateAffectation(row.getDateAffectation())
                                .dateDesaffectation(row.getDateDesaffectation())
                                .role(ReferenceDTO.builder()
                                                .id(row.getRoleId())
                                                .code(row.getRoleCode())
                                                .libelle(row.getRoleLibelle())
                                                .build())
                                .utilisateur(ReferenceDTO.builder()
                                                .id(row.getUtilisateurId())
                                                .libelle(row.getUtilisateurPrenom() + " " + row.getUtilisateurNom())
                                                .build())
                                .active(row.getDateDesaffectation() == null)
                                .build();
        }

        private String motif(String recherche) {
                return (recherche == null || recherche.isBlank())
                                ? null
                                : "%" + recherche.trim() + "%";
        }

        private Integer moisDebut(Integer trimestre) {
                if (trimestre == null || trimestre < 1 || trimestre > 4) {
                        return null;
                }
                return (trimestre - 1) * 3 + 1;
        }

        private Integer moisFin(Integer trimestre) {
                if (trimestre == null || trimestre < 1 || trimestre > 4) {
                        return null;
                }
                return (trimestre - 1) * 3 + 3;
        }

        private String codesTerminaux() {
                return String.join(",",
                                mg.bank.backend.enums.StatutActiviteEnum.codesTerminaux());
        }
}