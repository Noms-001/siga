package mg.bank.backend.mapper;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.EtapeValidationResponse;
import mg.bank.backend.dto.backoffice.PosteDecideurResponse;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.model.Poste;

public final class EtapeValidationMapper {

    private EtapeValidationMapper() {}

    public static EtapeValidationResponse toResponse(EtapeValidation e) {
        if (e == null) return null;

        List<PosteDecideurResponse> decideurs = e.getPostesDecideurs().stream()
                .map(EtapeValidationMapper::toPoste)
                .sorted(Comparator.comparing(PosteDecideurResponse::getNom,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return EtapeValidationResponse.builder()
                .id(e.getIdEtapeValidation())
                .designation(e.getDesignation())
                .description(e.getDescription())
                .niveau(e.getNiveau())
                .obligatoire(e.getObligatoire())
                .actif(e.getActif())
                .retour(e.getRetour())
                .dateDesactivation(e.getDateDesactivation())
                .decideurs(decideurs)
                .build();
    }

    private static PosteDecideurResponse toPoste(Poste p) {
        return PosteDecideurResponse.builder()
                .id(p.getIdPoste())
                .nom(p.getLibelle())     // colonne poste.nom
                .isMetier(p.getIsMetier())
                .build();
    }
}