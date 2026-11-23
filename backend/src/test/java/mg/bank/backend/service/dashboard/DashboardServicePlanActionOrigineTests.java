package mg.bank.backend.service.dashboard;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.dashboard.DashboardPlanActionOrigineDTO;
import mg.bank.backend.repository.dashboard.DashboardRepository;
import mg.bank.backend.repository.projection.dashboard.DashboardPlanActionOrigineRow;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.service.common.PeriodeSupport.Periode;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class DashboardServicePlanActionOrigineTests {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private DashboardService service;

    private static DashboardPlanActionOrigineRow ligne(
            String origine, long total, double avancement) {
        return new DashboardPlanActionOrigineRow() {
            @Override
            public String getOrigine() {
                return origine;
            }

            @Override
            public Long getTotal() {
                return total;
            }

            @Override
            public Double getAvancement() {
                return avancement;
            }
        };
    }

    private static Periode periode(LocalDate debut, LocalDate fin) {
        return new Periode(debut, fin, fin);
    }

    @Test
    void agregeLAvancementParOrigineEtCompleteLesOriginesAbsentes() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(currentUserService.getPerimetre())
                .thenReturn(PerimetreUtilisateur.builder().build());
        when(dashboardRepository.avancementPlansParOrigine(debut, fin, fin, null, null, null))
                .thenReturn(List.of(
                        ligne("INCIDENT", 4, 75.0),
                        ligne("AUTRE", 1, 0.0)));

        var dto = service.avancementPlansParOrigine(periode(debut, fin));

        assertEquals(
                List.of("INCIDENT", "RISQUE", "AUTRE"),
                dto.stream()
                        .map(DashboardPlanActionOrigineDTO::getOrigine)
                        .toList());

        var incident = dto.get(0);
        assertEquals("Incident", incident.getLibelle());
        assertEquals(4, incident.getTotal());
        assertEquals(75.0, incident.getAvancement(), 0.001);

        var risque = dto.get(1);
        assertEquals(0, risque.getTotal());
        assertEquals(0.0, risque.getAvancement(), 0.001);

        var autre = dto.get(2);
        assertEquals(1, autre.getTotal());
        assertEquals(0.0, autre.getAvancement(), 0.001);
    }

    @Test
    void retourneLesTroisOriginesQuandAucunPlanNeChevaucheLaPeriode() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 3, 31);
        when(currentUserService.getPerimetre())
                .thenReturn(PerimetreUtilisateur.builder().build());
        when(dashboardRepository.avancementPlansParOrigine(debut, fin, fin, null, null, null))
                .thenReturn(List.of());

        var dto = service.avancementPlansParOrigine(periode(debut, fin));

        assertEquals(3, dto.size());
        assertEquals(
                List.of("INCIDENT", "RISQUE", "AUTRE"),
                dto.stream()
                        .map(DashboardPlanActionOrigineDTO::getOrigine)
                        .toList());
        assertEquals(
                List.of(0L, 0L, 0L),
                dto.stream()
                        .map(DashboardPlanActionOrigineDTO::getTotal)
                        .toList());
    }
}
