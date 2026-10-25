package mg.bank.backend.repository.validation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.validation.Procedure;

public interface ProcedureRepository extends JpaRepository<Procedure, Integer> {

    List<Procedure> findByActifTrue();

    boolean existsByDesignationIgnoreCase(String designation);

    boolean existsByDesignationIgnoreCaseAndIdProcedureNot(String designation, Integer idProcedure);
}