package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.bank.backend.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    List<Role> findByActifTrue();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdRoleNot(String code, Integer idRole);
}
