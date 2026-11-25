package co.medicalprinter.dto;

import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Rol;
import co.medicalprinter.domain.TipoDestinatario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de entrada y salida. Coinciden con openapi.yaml. */
public final class Dtos {
    private Dtos() {}

    // ----- autenticación -----
    public record LoginRequest(@NotBlank @Email String correo, @NotBlank @Size(min = 8) String password) {}

    public record RegistroRequest(
            @NotBlank @Size(min = 2, max = 120) String nombre,
            @NotBlank @Email @Size(max = 150) String correo,
            @Size(max = 20) String telefono,
            @NotBlank @Size(min = 8) String password,
            @Positive Long empresaId) {}

    public record UsuarioResumen(Long id, String nombre, Rol rol) {}

    public record AuthResponse(String token, long expiraEnMinutos, UsuarioResumen usuario) {}

    // ----- productos -----
    public record ProductoDto(Long id, String nombre, String descripcion, BigDecimal precio, int stock,
                              EstadoProducto estado, String imagenUrl, Long categoriaId) {}

    public record ProductoPage(List<ProductoDto> contenido, int pagina, int tamano, long totalElementos) {}

    public record ProductoUpdate(
            @NotBlank @Size(max = 150) String nombre,
            String descripcion,
            @NotNull @DecimalMin("0.0") BigDecimal precio,
            @NotNull @Min(0) Integer stock,
            @NotNull EstadoProducto estado) {}

    // ----- empresas -----
    public record EmpresaRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 20) String nit,
            @Size(max = 20) String telefono,
            @Size(max = 150) String correo,
            @Size(max = 200) String direccion,
            @Size(max = 100) String ciudad) {}

    public record EmpresaDto(Long empresaId, String nombre, String nit) {}

    // ----- pedidos -----
    public record ItemPedidoRequest(
            @NotNull @Positive Long productoId,
            @NotNull @Min(1) @Max(1000) Integer cantidad) {}

    public record PedidoRequest(
            @NotNull TipoDestinatario destinatarioTipo,
            Long empresaId,
            @Size(max = 150) String contacto,
            @NotEmpty @Valid List<ItemPedidoRequest> items) {}

    public record ItemDto(Long productoId, String producto, int cantidad, BigDecimal precioUnitario) {}

    public record PedidoDto(Long pedidoId, EstadoPedido estado, BigDecimal total, LocalDateTime createdAt,
                            String destinatario, List<ItemDto> items) {}

    public record CambioEstadoRequest(@NotNull EstadoPedido estado) {}

    public record CambioEstadoResponse(Long pedidoId, EstadoPedido estadoAnterior,
                                       EstadoPedido estadoNuevo, LocalDateTime fecha) {}

    // ----- facturas y panel -----
    public record FacturaDto(Long facturaId, Long numero, BigDecimal subtotal, BigDecimal impuestos,
                             BigDecimal total, Long pedidoId, LocalDateTime fechaEmision) {}

    public record ResumenDto(long pedidosTotal, long pedidosPendientes, long facturas,
                             long productosDisponibles, long usuarios) {}
}
