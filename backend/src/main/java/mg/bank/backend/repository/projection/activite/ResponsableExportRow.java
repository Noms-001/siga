package mg.bank.backend.repository.projection.activite;

public interface ResponsableExportRow {

    Integer getIdActivite();

    Integer getIdSousActivite();

    Integer getIdUtilisateur();

    String getNom();

    String getPrenom();
}
