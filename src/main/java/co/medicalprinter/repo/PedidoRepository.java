package co.medicalprinter.repo;

import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.Pedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);
    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
    List<Pedido> findAllByOrderByCreatedAtDesc();
    long countByEstado(EstadoPedido estado);
}
