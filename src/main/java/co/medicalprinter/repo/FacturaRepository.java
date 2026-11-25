package co.medicalprinter.repo;

import co.medicalprinter.domain.Factura;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    boolean existsByPedidoId(Long pedidoId);
    List<Factura> findAllByOrderByNumeroDesc();
}
