package mg.bank.backend.service.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;

import mg.bank.backend.exception.ApiException;

/**
 * Resolution de la periode temporelle du dashboard.
 *
 * Une seule periode est resolue par requete puis transmise a toutes les
 * requetes SQL. Le front envoie dateDebut/dateFin ; le parametre annee
 * reste accepte pour compatibilite (anciens appels) et pour les appels
 * sans filtre explicite.
 */
public final class PeriodeSupport {

    /** Garde-fou : evite de balayer plusieurs decennies. */
    private static final int MOIS_MAX = 120;

    private PeriodeSupport() {
    }

    /**
     * @param debut     premier jour de la periode (inclus)
     * @param fin       dernier jour de la periode (inclus)
     * @param reference date a laquelle on reconstitue statuts et
     *                  avancements : min(fin, aujourd'hui)
     */
    public record Periode(LocalDate debut, LocalDate fin, LocalDate reference) {
    }

    public static Periode resoudre(
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer annee) {

        LocalDate debut;
        LocalDate fin;

        if (dateDebut != null || dateFin != null) {
            if (dateDebut == null || dateFin == null) {
                throw new ApiException(
                        "Les dates de debut et de fin doivent etre fournies ensemble",
                        HttpStatus.BAD_REQUEST);
            }
            debut = dateDebut;
            fin = dateFin;
        } else if (annee != null) {
            debut = LocalDate.of(annee, 1, 1);
            fin = LocalDate.of(annee, 12, 31);
        } else {
            int courante = LocalDate.now().getYear();
            debut = LocalDate.of(courante, 1, 1);
            fin = LocalDate.of(courante, 12, 31);
        }

        if (debut.isAfter(fin)) {
            throw new ApiException(
                    "La date de debut doit etre anterieure ou egale a la date de fin",
                    HttpStatus.BAD_REQUEST);
        }

        long nombreMois = ChronoUnit.MONTHS.between(
                debut.withDayOfMonth(1),
                fin.withDayOfMonth(1)) + 1;
        if (nombreMois > MOIS_MAX) {
            throw new ApiException(
                    "La periode ne peut pas depasser " + MOIS_MAX + " mois",
                    HttpStatus.BAD_REQUEST);
        }

        LocalDate aujourdHui = LocalDate.now();
        LocalDate reference = fin.isAfter(aujourdHui) ? aujourdHui : fin;

        return new Periode(debut, fin, reference);
    }
}
