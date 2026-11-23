package mg.bank.backend.enums;

/**
 * Portees du plus restrictif au plus large. L'ordre est utilise par
 * {@link mg.bank.backend.service.common.PorteeService#porteeAuMoins}.
 */
public enum PorteePermission {
    UTILISATEUR,
    SERVICE,
    DEPARTEMENT,
    TOUS
}