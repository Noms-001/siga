package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Compteurs affiches dans les cards de la page.
 *
 * Calcules par la meme clause WHERE que la liste, sur le meme perimetre,
 * pour que les cards et le tableau ne puissent pas diverger.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActiviteStatistiquesDTO {

    private long total;
    private long nonCommencees;
    private long enCours;
    private long terminees;
    private long enRetard;
    private long annulees;
    private long reportees;
    private long suspendues;

}
