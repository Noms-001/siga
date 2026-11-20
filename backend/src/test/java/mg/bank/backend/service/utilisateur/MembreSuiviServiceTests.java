package mg.bank.backend.service.utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.referentiel.Departement;
import mg.bank.backend.model.referentiel.Service;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.utilisateur.MembreSuiviRepository;

@ExtendWith(MockitoExtension.class)
class MembreSuiviServiceTests {

    @Mock
    private MembreSuiviRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private MembreSuiviService service;

    @BeforeEach
    void setup() {
        lenient().when(repository.findCategories(any())).thenReturn(List.of(
                Map.of("categorie", "PTA", "total", 2L, "termines", 1L, "en_cours", 1L,
                        "en_retard", 0L, "avancement", new BigDecimal("50.00")),
                Map.of("categorie", "NON_PTA", "total", 1L, "termines", 1L, "en_cours", 0L,
                        "en_retard", 0L, "avancement", new BigDecimal("100.00")),
                Map.of("categorie", "PLANS_ACTIONS", "total", 1L, "termines", 0L, "en_cours", 1L,
                        "en_retard", 0L, "avancement", new BigDecimal("0.00"))));
        lenient().when(repository.findStatutsActivites(any())).thenReturn(List.of());
        lenient().when(repository.findEvolution(any())).thenReturn(List.of());
        lenient().when(repository.findServices(any())).thenReturn(List.of());
        lenient().when(repository.countPlansMultiServices(any())).thenReturn(0L);
        lenient().when(repository.countMembres(any())).thenReturn(3L);
        lenient().when(repository.findMembres(any(), org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(List.of(
                Map.ofEntries(
                        Map.entry("id_utilisateur", 1), Map.entry("nom", "Rabe"), Map.entry("prenom", "Aina"),
                        Map.entry("id_service", 12), Map.entry("service", "Service A"),
                        Map.entry("total_activites", 2L), Map.entry("total_pta", 1L),
                        Map.entry("total_non_pta", 1L), Map.entry("avancement_activites", new BigDecimal("40.00")),
                        Map.entry("total_plans", 1L), Map.entry("avancement_plans", new BigDecimal("60.00")),
                        Map.entry("avancement_global", new BigDecimal("46.67")), Map.entry("elements_en_retard", 1L)),
                Map.ofEntries(
                        Map.entry("id_utilisateur", 2), Map.entry("nom", "Raso"), Map.entry("prenom", "Bema"),
                        Map.entry("id_service", 12), Map.entry("service", "Service A"),
                        Map.entry("total_activites", 0L), Map.entry("total_pta", 0L),
                        Map.entry("total_non_pta", 0L), Map.entry("total_plans", 2L),
                        Map.entry("avancement_plans", new BigDecimal("75.00")),
                        Map.entry("avancement_global", new BigDecimal("75.00")), Map.entry("elements_en_retard", 0L)),
                Map.ofEntries(
                        Map.entry("id_utilisateur", 3), Map.entry("nom", "Solo"), Map.entry("prenom", "Cely"),
                        Map.entry("id_service", 12), Map.entry("service", "Service A"),
                        Map.entry("total_activites", 0L), Map.entry("total_pta", 0L),
                        Map.entry("total_non_pta", 0L), Map.entry("total_plans", 0L),
                        Map.entry("elements_en_retard", 0L))));
        lenient().when(serviceRepository.findOptionsActivesParDepartement(7)).thenReturn(List.of());
    }

    @Test
    void globalPondereLesTroisCategoriesEtCompteChaqueElementUneFois() {
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                .idDepartement(7).build());

        var result = service.global(null, null, 2026, null, null, null);

        assertEquals(2, result.totalActivitesPta());
        assertEquals(1, result.totalActivitesNonPta());
        assertEquals(1, result.totalPlansActions());
        assertEquals(new BigDecimal("50.00"), result.avancementGlobal());
        assertEquals(3, result.nombreMembres());
        assertEquals(3, result.categories().size());
        assertTrue(result.comparaisonServicesVisible());
    }

        @Test
        void resumeMembresCalculeLesQuatreIndicateursSurLesMembresFiltres() {
                when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                                .idDepartement(7).build());

                var result = service.membres(null, null, 2026, null, null, null);

                assertEquals(3, result.membresSuivis());
                assertEquals(1, result.membresAvecActivites());
                assertEquals(2, result.membresAvecPlansActions());
                assertEquals(1, result.membresAvecRetards());
                assertEquals(1, result.membres().get(0).totalPta());
                assertEquals(1, result.membres().get(0).totalNonPta());
                assertEquals(new BigDecimal("40.00"), result.membres().get(0).avancementActivites());
                assertEquals(new BigDecimal("60.00"), result.membres().get(0).avancementPlansActions());
                assertNull(result.membres().get(2).avancementActivites());
                assertNull(result.membres().get(2).avancementPlansActions());
                verify(repository).findMembres(any(), org.mockito.ArgumentMatchers.isNull());
        }

    @Test
    void filtreCategorieNeutraliseLesAutresSeriesSansLesFusionner() {
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                .idService(12).idDepartement(7).build());

        var result = service.global(null, null, 2026, null, null, "pta");

        assertEquals(2, result.totalActivitesPta());
        assertEquals(0, result.totalActivitesNonPta());
        assertEquals(0, result.totalPlansActions());
        assertEquals(new BigDecimal("50.00"), result.avancementGlobal());
        assertFalse(result.comparaisonServicesVisible());
        assertEquals("PTA", result.categories().get(0).categorie());
        assertEquals(BigDecimal.ZERO, result.categories().get(1).avancement());
        assertEquals(0, result.statutsActivites().stream().mapToLong(row -> row.nombre()).sum());
    }

    @Test
    void chefDeServiceNePeutPasDemanderUnAutreService() {
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                .idService(12).idDepartement(7).build());

        ApiException error = assertThrows(ApiException.class,
                () -> service.global(null, null, 2026, null, 99, null));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(repository, never()).findCategories(any());
    }

        @Test
        void listeMembresRefuseUnServiceHorsPerimetreAvantLaRequete() {
                when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                                .idService(12).idDepartement(7).build());

                ApiException error = assertThrows(ApiException.class,
                                () -> service.membres(null, null, 2026, null, 99, null));

                assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
                verify(repository, never()).findMembres(any(), org.mockito.ArgumentMatchers.nullable(String.class));
        }

    @Test
    void chefDeDepartementNePeutPasFiltrerUnServiceHorsDepartement() {
        Departement autreDepartement = Departement.builder().idDepartement(8).build();
        Service autreService = Service.builder().idService(99).departement(autreDepartement).build();
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                .idDepartement(7).build());
        when(serviceRepository.findById(99)).thenReturn(Optional.of(autreService));

        ApiException error = assertThrows(ApiException.class,
                () -> service.global(null, null, 2026, null, 99, null));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(repository, never()).findCategories(any());
    }

    @Test
    void refuseUnUtilisateurSansPerimetre() {
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder().build());

        ApiException error = assertThrows(ApiException.class,
                () -> service.global(null, null, 2026, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void validePeriodesEtCategories() {
        when(currentUserService.getPerimetre()).thenReturn(PerimetreUtilisateur.builder()
                .idDepartement(7).build());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.global(LocalDate.of(2026, 2, 1), null, null, null, null, null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.global(null, null, 2026, 5, null, null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.global(null, null, 2026, null, null, "AUTRE")).getStatus());
    }
}
