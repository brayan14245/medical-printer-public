package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.HistorialEstado;
import co.medicalprinter.domain.Pedido;
import co.medicalprinter.domain.Producto;
import co.medicalprinter.repo.FacturaRepository;
import co.medicalprinter.repo.HistorialEstadoRepository;
import co.medicalprinter.repo.PedidoRepository;
import co.medicalprinter.repo.ProductoRepository;
import co.medicalprinter.repo.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoEstadoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final FacturaRepository facturas;
    private final HistorialEstadoRepository historial;
    private final UsuarioRepository usuarios;

    /** HU-03: RN-06 (solo ADMIN, en la capa de seguridad), RN-07 y RN-08. */
    @Transactional
    public CambioEstadoResponse cambiar(Long pedidoId, Long adminId, EstadoPedido nuevo) {
        Pedido p = pedidos.findById(pedidoId)
                .orElseThrow(() -> BusinessException.noEncontrado("El pedido no existe"));
        EstadoPedido anterior = p.getEstado();

        if (!anterior.puedePasarA(nuevo)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSICION_INVALIDA",
                    "Transición de estado no permitida", "RN-07");
        }
        if (nuevo == EstadoPedido.CANCELADO) {
            if (facturas.existsByPedidoId(pedidoId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "PEDIDO_FACTURADO",
                        "El pedido ya tiene factura, anúlala antes de cancelarlo", "RN-07");
            }
            reintegrarStock(p); // RN-08
        }

        p.setEstado(nuevo);
        p.setUpdatedAt(LocalDateTime.now());

        HistorialEstado h = new HistorialEstado();
        h.setPedido(p);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setCambiadoPor(usuarios.getReferenceById(adminId));
        historial.save(h);

        pedidos.saveAndFlush(p); // @Version: si otro admin lo cambió, responde 409
        return new CambioEstadoResponse(p.getId(), anterior, nuevo, h.getFecha());
    }

    private void reintegrarStock(Pedido p) {
        List<Long> ids = p.getItems().stream().map(i -> i.getProducto().getId()).toList();
        List<Producto> bloqueados = productos.bloquearPorIds(ids);
        p.getItems().forEach(item -> bloqueados.stream()
                .filter(prod -> prod.getId().equals(item.getProducto().getId()))
                .findFirst()
                .ifPresent(prod -> prod.setStock(prod.getStock() + item.getCantidad())));
    }
}
