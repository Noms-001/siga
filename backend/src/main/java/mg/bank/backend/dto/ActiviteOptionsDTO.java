package mg.bank.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Referentiels des filtres, groupes par type.
 *
 * Regroupes explicitement : renvoyer une liste plate melangerait services,
 * priorites, types et sites, et le front ne pourrait pas les distinguer.
 *
 * services est vide pour un utilisateur rattache a un service : le filtre
 * n'est pas fourni plutot que masque.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActiviteOptionsDTO {

    private List<OptionDTO> services;
    private List<OptionDTO> priorites;
    private List<OptionDTO> typesActivite;
    private List<OptionDTO> sites;
    private List<OptionDTO> statuts;

}
