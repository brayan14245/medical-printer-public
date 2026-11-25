package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.Consecutivo;
import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.Factura;
import co.medicalprinter.domain.Pedido;
import co.medicalprinter.repo.ConsecutivoRepository;
import co.medicalprinter.repo.FacturaRepository;
import co.medicalprinter.repo.PedidoRepository;
import co.medicalprinter.repo.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacturaService {
    private static final String SERIE = "FACTURA";

    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final ConsecutivoRepository consecutivos;
    private final UsuarioRepository usuarios;

    @Value("${app.iva}")
    private BigDecimal iva;

    /** HU-04: RN-09, RN-10 y RNF-06. */
    @Transactional
    public FacturaDto generar(Long pedidoId, Long adminId) {
        Pedido p = pedidos.findById(pedidoId)
                .orElseThrow(() -> BusinessException.noEncontrado("El pedido no existe"));

        // RNF-06: se bloquea el consecutivo primero, así toda la facturación se serializa
        Consecutivo c = consecutivos.bloquear(SERIE)
                .orElseThrow(() -> new IllegalStateException("Falta la serie de facturas en consecutivos"));

        if (facturas.existsByPedidoId(pedidoId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PEDIDO_YA_FACTURADO",
                    "Este pedido ya fue facturado", "RN-09");
        }
        if (p.getEstado() == EstadoPedido.PENDIENTE || p.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "ESTADO_NO_FACTURABLE",
                    "Solo se pueden facturar pedidos confirmados o superiores", "RN-09");
        }

        // RN-10: precios congelados del pedido, BigDecimal y redondeo HALF_UP
        BigDecimal subtotal = p.getItems().stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal impuestos = subtotal.multiply(iva).setScale(2, RoundingMode.HALF_UP);

        long numero = c.getUltimoNumero() + 1;
        c.setUltimoNumero(numero);

        Factura f = new Factura();
        f.setPedido(p);
        f.setNumero(numero);
        f.setSubtotal(subtotal);
        f.setImpuestos(impuestos);
        f.setTotal(subtotal.add(impuestos));
        f.setEmitidaPor(usuarios.getReferenceById(adminId));
        try {
            facturas.saveAndFlush(f); // UNIQUE(pedido_id): si falla, se revierte también el consecutivo
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "PEDIDO_YA_FACTURADO",
                    "Este pedido ya fue facturado", "RN-09");
        }
        return aDto(f);
    }

    @Transactional(readOnly = true)
    public List<FacturaDto> listar() {
        return facturas.findAllByOrderByNumeroDesc().stream().map(FacturaService::aDto).toList();
    }

    private static FacturaDto aDto(Factura f) {
        return new FacturaDto(f.getId(), f.getNumero(), f.getSubtotal(), f.getImpuestos(), f.getTotal(),
                f.getPedido().getId(), f.getFechaEmision());
    }
}
