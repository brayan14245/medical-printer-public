package co.medicalprinter.web;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.repo.FacturaRepository;
import co.medicalprinter.repo.PedidoRepository;
import co.medicalprinter.repo.ProductoRepository;
import co.medicalprinter.repo.UsuarioRepository;
import co.medicalprinter.service.FacturaService;
import co.medicalprinter.service.PedidoEstadoService;
import co.medicalprinter.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Todas las rutas /admin/** exigen rol ADMIN (RN-06, definido en SecurityConfig). */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PedidoService pedidos;
    private final PedidoEstadoService estados;
    private final FacturaService facturas;
    private final PedidoRepository pedidoRepo;
    private final FacturaRepository facturaRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return new ResumenDto(pedidoRepo.count(), pedidoRepo.countByEstado(EstadoPedido.PENDIENTE),
                facturaRepo.count(), productoRepo.countByEstado(EstadoProducto.DISPONIBLE), usuarioRepo.count());
    }

    @GetMapping("/pedidos")
    public List<PedidoDto> pedidos() {
        return pedidos.todos();
    }

    @PatchMapping("/pedidos/{pedidoId}/estado")
    public CambioEstadoResponse cambiarEstado(@PathVariable Long pedidoId,
                                              @AuthenticationPrincipal Long adminId,
                                              @Valid @RequestBody CambioEstadoRequest r) {
        return estados.cambiar(pedidoId, adminId, r.estado());
    }

    @PostMapping("/pedidos/{pedidoId}/factura")
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaDto facturar(@PathVariable Long pedidoId, @AuthenticationPrincipal Long adminId) {
        return facturas.generar(pedidoId, adminId);
    }

    @GetMapping("/facturas")
    public List<FacturaDto> facturas() {
        return facturas.listar();
    }
}
