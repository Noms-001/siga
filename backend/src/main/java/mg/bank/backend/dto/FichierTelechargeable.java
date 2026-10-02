package mg.bank.backend.dto;

/**
 * Contenu binaire d un fichier pret a etre renvoye.
 *
 * Porte le nom d origine et le type MIME pour les en-tetes HTTP, et les
 * octets pour le corps. Le chemin de stockage ne sort jamais d ici : la
 * couche de telechargement le lit pour produire le contenu, et rien d autre.
 */
public record FichierTelechargeable(
        String nomOriginal,
        String typeMime,
        byte[] contenu) {
}