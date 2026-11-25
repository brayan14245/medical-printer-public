package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.Empresa;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Pedido;
import co.medicalprinter.domain.PedidoItem;
import co.medicalprinter.domain.Producto;
import co.medicalprinter.domain.TipoDestinatario;
import co.medicalprinter.domain.Usuario;
import co.medicalprinter.repo.EmpresaRepository;
import co.medicalprinter.repo.PedidoRepository;
import co.medicalprinter.repo.ProductoRepository;
import co.medicalprinter.repo.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final ProductoRepository productos;
    private final PedidoRepository pedidos;
    private final ApplicationEventPublisher eventos;

    /** HU-02: RN-03, RN-04, RN-05, RNF-02, RNF-04, RNF-05. Todo ocurre en una sola transacción. */
    @Transactional
    public PedidoDto crear(Long usuarioId, String claveIdempotencia, PedidoRequest r) {
        // RNF-05: la misma clave devuelve el pedido ya creado
        if (claveIdempotencia != null && !claveIdempotencia.isBlank()) {
            var existente = pedidos.findByIdempotencyKey(claveIdempotencia);
            if (existente.isPresent() && existente.get().getUsuario().getId().equals(usuarioId)) {
                return aDto(existente.get());
            }
        }

        // RN-04: destinatario y contacto
        if (r.contacto() == null || r.contacto().isBlank()) {
            throw destinatarioRequerido("Debes indicar un contacto para el pedido");
        }
        Empresa empresa = null;
        if (r.destinatarioTipo() == TipoDestinatario.EMPRESA) {
            if (r.empresaId() == null) {
                throw destinatarioRequerido("Debes seleccionar una empresa registrada");
            }
            empresa = empresas.findById(r.empresaId())
                    .orElseThrow(() -> destinatarioRequerido("La empresa indicada no está registrada"));
        }

        // agrupar productos repetidos
        Map<Long, Integer> cantidades = new TreeMap<>();
        r.items().forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));

        // RNF-02: bloqueo pesimista de los productos, ordenados por id
        List<Producto> bloqueados = productos.bloquearPorIds(cantidades.keySet());
        if (bloqueados.size() != cantidades.size()) {
            throw BusinessException.noEncontrado("Uno de los productos del pedido no existe");
        }

        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> BusinessException.noEncontrado("El usuario no existe"));
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDestinatarioTipo(r.destinatarioTipo());
        pedido.setEmpresa(empresa);
        pedido.setContacto(r.contacto().trim());
        pedido.setIdempotencyKey(claveIdempotencia == null || claveIdempotencia.isBlank() ? null : claveIdempotencia);

        BigDecimal total = BigDecimal.ZERO;
        for (Producto prod : bloqueados) {
            int cantidad = cantidades.get(prod.getId());
            // RN-03
            if (prod.getEstado() != EstadoProducto.DISPONIBLE) {
                throw new BusinessException(HttpStatus.CONFLICT, "PRODUCTO_NO_DISPONIBLE",
                        "El producto " + prod.getNombre() + " ya no está disponible", "RN-03");
            }
            if (cantidad > prod.getStock()) {
                throw new BusinessException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                        "Stock insuficiente, máximo disponible: " + prod.getStock(), "RN-03");
            }
            prod.setStock(prod.getStock() - cantidad);

            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(prod);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(prod.getPrecio()); // precio congelado (RN-10)
            pedido.getItems().add(item);
            total = total.add(prod.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        }
        pedido.setTotal(total);

        try {
            pedidos.saveAndFlush(pedido); // RN-05: pedido PENDIENTE y stock descontado atómicamente
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "PEDIDO_DUPLICADO",
                    "Este pedido ya fue enviado", null);
        }

        // RNF-04: la notificación se envía después del commit
        eventos.publishEvent(new PedidoCreadoEvent(pedido.getId(), destinatario(pedido), pedido.getContacto()));
        return aDto(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> misPedidos(Long usuarioId) {
        return pedidos.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream().map(PedidoService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> todos() {
        return pedidos.findAllByOrderByCreatedAtDesc().stream().map(PedidoService::aDto).toList();
    }

    private static BusinessException destinatarioRequerido(String mensaje) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "DESTINATARIO_REQUERIDO", mensaje, "RN-04");
    }

    private static String destinatario(Pedido p) {
        return p.getEmpresa() != null ? p.getEmpresa().getNombre() : p.getUsuario().getNombre();
    }

    public static PedidoDto aDto(Pedido p) {
        var items = p.getItems().stream()
                .map(i -> new ItemDto(i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return new PedidoDto(p.getId(), p.getEstado(), p.getTotal(), p.getCreatedAt(), destinatario(p), items);
    }
}
