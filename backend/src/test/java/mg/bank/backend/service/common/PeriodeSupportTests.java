package mg.bank.backend.service.common;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import mg.bank.backend.exception.ApiException;

class PeriodeSupportTests {

    @Test
    void utiliseLAnneeQuandLesDatesSontAbsentes() {
        var periode = PeriodeSupport.resoudre(null, null, 2026);

        assertEquals(LocalDate.of(2026, 1, 1), periode.debut());
        assertEquals(LocalDate.of(2026, 12, 31), periode.fin());
    }

    @Test
    void priviliegieLesDatesSurLAnnee() {
        var periode = PeriodeSupport.resoudre(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 4, 30),
                2020);

        assertEquals(LocalDate.of(2026, 2, 1), periode.debut());
        assertEquals(LocalDate.of(2026, 4, 30), periode.fin());
    }

    @Test
    void laReferenceEstBorneeAAujourdHuiPourUnePeriodeFuture() {
        LocalDate debut = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusMonths(1);

        var periode = PeriodeSupport.resoudre(debut, fin, null);

        assertEquals(LocalDate.now(), periode.reference());
    }

    @Test
    void laReferenceEstLaFinPourUnePeriodePassee() {
        LocalDate debut = LocalDate.of(2020, 1, 1);
        LocalDate fin = LocalDate.of(2020, 12, 31);

        var periode = PeriodeSupport.resoudre(debut, fin, null);

        assertEquals(fin, periode.reference());
    }

    @Test
    void refuseUnePeriodeInversee() {
        assertThrows(ApiException.class, () -> PeriodeSupport.resoudre(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void refuseUnePeriodeTropLongue() {
        assertThrows(ApiException.class, () -> PeriodeSupport.resoudre(
                LocalDate.of(2016, 1, 1), LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void refuseUneSeuleDesDeuxDates() {
        assertThrows(ApiException.class, () -> PeriodeSupport.resoudre(
                null, LocalDate.of(2026, 1, 1), null));
        assertThrows(ApiException.class, () -> PeriodeSupport.resoudre(
                LocalDate.of(2026, 1, 1), null, null));
    }
}
