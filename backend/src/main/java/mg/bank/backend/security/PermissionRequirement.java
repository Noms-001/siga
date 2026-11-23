package mg.bank.backend.security;

/**
 * Permission attendue : une ressource et une action.
 */
public record PermissionRequirement(String ressource, String action) {
}
