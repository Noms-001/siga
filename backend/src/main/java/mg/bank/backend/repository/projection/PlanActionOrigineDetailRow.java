// PlanActionOrigineDetailRow.java
package mg.bank.backend.repository.projection;

public interface PlanActionOrigineDetailRow {
    Integer getIdOrigine();
    String getCode();
    String getDesignation();
    String getTypeOrigine();
    Boolean getEstPrincipale();
}