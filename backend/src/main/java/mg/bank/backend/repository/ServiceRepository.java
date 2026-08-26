package mg.bank.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Service;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    
}
