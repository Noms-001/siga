package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.ValeurIndicateurResponse;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.model.ValeurIndicateur;

public final class ValeurIndicateurMapper {

    private ValeurIndicateurMapper() {}

    public static ValeurIndicateurResponse toResponse(ValeurIndicateur v) {
        if (v == null) return null;

        Utilisateur u = v.getUtilisateur();
        return ValeurIndicateurResponse.builder()
                .id(v.getIdValeurIndicateur())
                .valeur(v.getValeur() == null ? null : v.getValeur().doubleValue())
                .periodeDebut(v.getPeriodeDebut())
                .periodeFin(v.getPeriodeFin())
                .commentaire(v.getCommentaire())
                .dateSaisie(v.getDateSaisie())
                .idUtilisateur(u == null ? null : u.getIdUtilisateur())
                .nomUtilisateur(u == null ? null : u.getNom())
                .prenomUtilisateur(u == null ? null : u.getPrenom())
                .build();
    }
}