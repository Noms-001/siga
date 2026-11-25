package mg.bank.backend.service.planaction;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.export.PlanActionExportResponse;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.model.referentiel.Departement;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.planaction.PlanActionExportRepository;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.security.PorteeAcces;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class PlanActionExportServiceTests {

    @Mock
    private PlanActionExportRepository planActionExportRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PlanActionExportService service;

    @Test
    void exporteTousLesPlansAvecLaPorteeTous() {
        Utilisateur utilisateur = Utilisateur.builder()
                .idUtilisateur(1)
                .departement(Departement.builder().idDepartement(2).build())
                .build();
        PerimetreUtilisateur perimetre = PerimetreUtilisateur.builder()
                .porteeAcces(PorteeAcces.de(PorteePermission.TOUS, 1, null, 2))
                .build();

        when(currentUserService.get()).thenReturn(utilisateur);
        when(currentUserService.getPerimetre()).thenReturn(perimetre);
        when(planActionExportRepository.findPlansPourExport(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq("NON_COMMENCEE,EN_COURS,TERMINEE"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        PlanActionExportResponse response = service.exporter(null);

        assertNull(response.getDepartement());
        assertTrue(response.getServices().isEmpty());
        verify(planActionExportRepository).findPlansPourExport(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq("NON_COMMENCEE,EN_COURS,TERMINEE"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
    }
}
