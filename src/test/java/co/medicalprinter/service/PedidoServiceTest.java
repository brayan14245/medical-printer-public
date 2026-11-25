package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.ItemPedidoRequest;
import static co.medicalprinter.dto.Dtos.PedidoRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Pedido;
import co.medicalprinter.domain.Producto;
import co.medicalprinter.domain.TipoDestinatario;
import co.medicalprinter.domain.Usuario;
import co.medicalprinter.repo.EmpresaRepository;
import co.medicalprinter.repo.PedidoRepository;
import co.medicalprinter.repo.ProductoRepository;
import co.medicalprinter.repo.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock private UsuarioRepository usuarios;
    @Mock private EmpresaRepository empresas;
    @Mock private ProductoRepository productos;
    @Mock private PedidoRepository pedidos;
    @Mock private ApplicationEventPublisher eventos;
    @InjectMocks private PedidoService servicio;

    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void prepararDatos() {
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Laura Martínez");

        producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Monitor clínico");
        producto.setPrecio(new BigDecimal("12.50"));
        producto.setStock(5);
        producto.setEstado(EstadoProducto.DISPONIBLE);
    }

    @Test
    void creaPedidoPendienteCalculaTotalDescuentaStockYPublicaEvento() {
        // Arrange
        when(pedidos.findByIdempotencyKey("pedido-1")).thenReturn(Optional.empty());
        when(productos.bloquearPorIds(Set.of(10L))).thenReturn(List.of(producto));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(pedidos.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(42L);
            return pedido;
        });
        PedidoRequest solicitud = solicitud(List.of(
                new ItemPedidoRequest(10L, 1),
                new ItemPedidoRequest(10L, 2)));

        // Act
        var resultado = servicio.crear(7L, "pedido-1", solicitud);

        // Assert
        assertEquals(42L, resultado.pedidoId());
        assertEquals(EstadoPedido.PENDIENTE, resultado.estado());
        assertEquals(0, new BigDecimal("37.50").compareTo(resultado.total()));
        assertEquals(2, producto.getStock());
        assertEquals(1, resultado.items().size());
        assertEquals(3, resultado.items().get(0).cantidad());
        verify(eventos).publishEvent(any(PedidoCreadoEvent.class));
    }

    @Test
    void rechazaPedidoCuandoNoHayStockSuficiente() {
        // Arrange
        when(pedidos.findByIdempotencyKey("pedido-2")).thenReturn(Optional.empty());
        when(productos.bloquearPorIds(Set.of(10L))).thenReturn(List.of(producto));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        PedidoRequest solicitud = solicitud(List.of(new ItemPedidoRequest(10L, 6)));

        // Act
        BusinessException error = assertThrows(
                BusinessException.class, () -> servicio.crear(7L, "pedido-2", solicitud));

        // Assert
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("STOCK_INSUFICIENTE", error.getCodigo());
        assertEquals(5, producto.getStock());
        verify(pedidos, never()).saveAndFlush(any(Pedido.class));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void devuelve404CuandoUnProductoNoExiste() {
        // Arrange
        when(pedidos.findByIdempotencyKey("pedido-3")).thenReturn(Optional.empty());
        when(productos.bloquearPorIds(anySet())).thenReturn(List.of());
        PedidoRequest solicitud = solicitud(List.of(new ItemPedidoRequest(999L, 1)));

        // Act
        BusinessException error = assertThrows(
                BusinessException.class, () -> servicio.crear(7L, "pedido-3", solicitud));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        assertEquals("NO_ENCONTRADO", error.getCodigo());
        verify(usuarios, never()).findById(7L);
        verify(pedidos, never()).saveAndFlush(any(Pedido.class));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    private static PedidoRequest solicitud(List<ItemPedidoRequest> items) {
        return new PedidoRequest(TipoDestinatario.PERSONA, null, "  3105550142  ", items);
    }
}
