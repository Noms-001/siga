package mg.bank.backend.repository.utilisateur;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.utilisateur.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    List<Role> findByActifTrue();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdRoleNot(String code, Integer idRole);

    Optional<Role> findByCodeIgnoreCase(String code);

    List<Role> findByCodeIn(List<String> codes);
}
