package mg.bank.backend.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Statuts metier de l'activite, avec leur caractere terminal.
 *
 * Les codes ne sont pas reecrits ici : ils viennent de StatutsActivite, qui
 * est aussi la source utilisee par le SQL des statistiques.
 *
 * Cette enum ne liste QUE les statuts qu elle a quelque chose a dire. Elle
 * n est pas le catalogue des statuts : NON_COMMENCEE et SUSPENDUE en sont
 * absents parce qu ils ne sont pas terminaux, ce qui est une information, et
 * non un oubli. Les y ajouter avec terminal = false n aiderait personne et
 * donnerait l illusion d un catalogue complet, alors qu un code oublie ici
 * devient silencieusement non terminal.
 */
public enum StatutActiviteEnum {

    EN_COURS(StatutsActivite.EN_COURS, "En cours", false),
    TERMINEE(StatutsActivite.TERMINEE, "Terminée", true),
    ANNULEE(StatutsActivite.ANNULEE, "Annulée", true),
    REPORTEE(StatutsActivite.REPORTEE, "Reportée", true);

    private final String code;
    private final String libelleParDefaut;
    private final boolean terminal;

    StatutActiviteEnum(String code, String libelleParDefaut, boolean terminal) {
        this.code = code;
        this.libelleParDefaut = libelleParDefaut;
        this.terminal = terminal;
    }

    public String getCode() {
        return code;
    }

    public String getLibelleParDefaut() {
        return libelleParDefaut;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static boolean estTerminal(String code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(s -> s.terminal && s.code.equalsIgnoreCase(code));
    }

    /**
     * Codes terminaux : une activite terminee, annulee ou reportee ne peut
     * plus etre consideree en retard.
     */
    public static Set<String> codesTerminaux() {
        return Arrays.stream(values())
                .filter(StatutActiviteEnum::isTerminal)
                .map(StatutActiviteEnum::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Codes terminaux chaines, injectes dans le SQL.
     */
    public static String codesTerminauxSql() {
        return String.join(",", codesTerminaux());
    }

}
