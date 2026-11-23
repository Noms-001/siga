package mg.bank.backend.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SqlFragmentsTests {

    @Test
    void laPorteeUtilisateurConserveUnLitteralBrouillonValide() {
        assertTrue(SqlFragments.PORTEE_UTILISATEUR.contains("spu.code = 'BROUILLON'"));
        assertFalse(SqlFragments.PORTEE_UTILISATEUR.contains("BROUILLONAND"));
    }

    @Test
    void laPorteeUtilisateurPlanConserveUnLitteralBrouillonValide() {
        assertTrue(SqlFragments.PORTEE_UTILISATEUR_PLAN.contains("spc.code = 'BROUILLON'"));
        assertFalse(SqlFragments.PORTEE_UTILISATEUR_PLAN.contains("BROUILLONAND"));
    }

    @Test
    void leFiltreIndicateurReutiliseLaPorteeUtilisateur() {
        assertTrue(SqlFragments.INDICATEUR_DANS_PERIMETRE
                .contains(SqlFragments.PORTEE_UTILISATEUR));
    }
}
