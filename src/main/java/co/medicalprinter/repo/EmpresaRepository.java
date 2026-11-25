package co.medicalprinter.repo;

import co.medicalprinter.domain.Empresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByNit(String nit);
    boolean existsByNit(String nit);
}
