// PlanActionOrigineDetailRow.java
package mg.bank.backend.repository.projection.planaction;

public interface PlanActionOrigineDetailRow {
    Integer getIdOrigine();
    String getCode();
    String getDesignation();
    String getTypeOrigine();
    Boolean getEstPrincipale();
}