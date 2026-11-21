package mg.bank.backend.repository.projection.planaction;

public interface OrigineExportRow {

    Integer getIdPlanAction();

    String getCode();

    String getDesignation();

    String getTypeOrigine();

    Boolean getEstPrincipale();
}
