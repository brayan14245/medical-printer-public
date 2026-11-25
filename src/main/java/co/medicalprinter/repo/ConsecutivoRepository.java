package co.medicalprinter.repo;

import co.medicalprinter.domain.Consecutivo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsecutivoRepository extends JpaRepository<Consecutivo, String> {
    /** RNF-06: serializa la facturación para no repetir ni dejar huecos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consecutivo c where c.serie = :serie")
    Optional<Consecutivo> bloquear(@Param("serie") String serie);
}
