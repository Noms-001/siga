package mg.bank.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PermissionPolicyTests {

    @Test
    void uneRessourceGlobaleExigeUnePorteeTotale() {
        assertTrue(PermissionPolicy.exigePorteeTotale(List.of(
                new PermissionRequirement(PermissionResource.POSTE, PermissionAction.CONSULTER))));
    }

    @Test
    void uneRessourceHierarchiqueNExigePasUnePorteeTotale() {
        assertFalse(PermissionPolicy.exigePorteeTotale(List.of(
                new PermissionRequirement(PermissionResource.ACTIVITE, PermissionAction.CONSULTER))));
    }

    @Test
    void unMelangeDeRessourcesNExigePasUnePorteeTotale() {
        assertFalse(PermissionPolicy.exigePorteeTotale(List.of(
                new PermissionRequirement(PermissionResource.POSTE, PermissionAction.CONSULTER),
                new PermissionRequirement(PermissionResource.ACTIVITE, PermissionAction.CONSULTER))));
    }

    @Test
    void uneListeVideNExigePasUnePorteeTotale() {
        assertFalse(PermissionPolicy.exigePorteeTotale(List.of()));
    }
}
