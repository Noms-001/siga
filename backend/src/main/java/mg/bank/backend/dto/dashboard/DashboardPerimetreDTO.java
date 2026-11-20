package mg.bank.backend.dto.dashboard;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.dto.common.OptionDTO;

/**
 * Perimetre de l'utilisateur connecte, expose au front pour construire
 * le filtre Service du graphique « types d'activite ».
 *
 * - DEPARTEMENT : filtre visible, options = services du departement ;
 * - SERVICE     : filtre masque, donnees bloquees sur son service ;
 * - GLOBAL      : filtre masque, donnees globales.
 */
@Getter
@Builder
public class DashboardPerimetreDTO {

    private String niveau;
    private Integer idService;
    private Integer idDepartement;
    private String service;
    private List<OptionDTO> services;
}
