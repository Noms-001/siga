package mg.bank.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Integer> {
    
}
