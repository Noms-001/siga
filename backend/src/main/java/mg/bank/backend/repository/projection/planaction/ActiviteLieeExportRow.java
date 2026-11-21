package mg.bank.backend.repository.projection.planaction;

public interface ActiviteLieeExportRow {

    Integer getIdPlanAction();

    Integer getIdActivite();

    String getCode();

    String getDesignation();

    Integer getServiceId();

    String getServiceNom();

    String getOrigineCode();

    String getTypeOrigine();
}
