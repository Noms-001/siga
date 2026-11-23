package mg.bank.backend.service.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mg.bank.backend.dto.common.PerimetreUtilisateur;

class PorteeServiceTests {

    private final PorteeService porteeService = new PorteeService(null, null, null, null);

    @Test
    void leRattachementServiceEstUtiliseSansPorteeExplicite() {
        PerimetreUtilisateur perimetre = PerimetreUtilisateur.builder()
                .idService(5)
                .idDepartement(2)
                .build();

        assertFalse(porteeService.porteeTous(perimetre));
        assertTrue(porteeService.departementAccessible(2, perimetre));
        assertFalse(porteeService.departementAccessible(3, perimetre));
    }

    @Test
    void leRattachementDepartementEstUtiliseSansPorteeExplicite() {
        PerimetreUtilisateur perimetre = PerimetreUtilisateur.builder()
                .idDepartement(2)
                .build();

        assertFalse(porteeService.porteeTous(perimetre));
        assertTrue(porteeService.departementAccessible(2, perimetre));
        assertFalse(porteeService.departementAccessible(3, perimetre));
    }

    @Test
    void unUtilisateurSansRattachementVoitTout() {
        PerimetreUtilisateur perimetre = PerimetreUtilisateur.builder().build();

        assertTrue(porteeService.porteeTous(perimetre));
    }
}
