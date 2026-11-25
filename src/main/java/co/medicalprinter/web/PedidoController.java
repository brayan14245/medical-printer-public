package co.medicalprinter.web;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService pedidos;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto crear(@AuthenticationPrincipal Long usuarioId,
                           @RequestHeader(value = "Idempotency-Key", required = false) String clave,
                           @Valid @RequestBody PedidoRequest r) {
        return pedidos.crear(usuarioId, clave, r);
    }

    @GetMapping("/mis")
    public List<PedidoDto> mis(@AuthenticationPrincipal Long usuarioId) {
        return pedidos.misPedidos(usuarioId);
    }
}
