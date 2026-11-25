package co.medicalprinter.web;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productos;

    /** Público: catálogo de la tienda. */
    @GetMapping("/productos")
    public ProductoPage listar(@RequestParam(required = false) EstadoProducto estado,
                               @RequestParam(required = false) Long categoriaId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size) {
        return productos.listar(estado, categoriaId, page, size);
    }

    @GetMapping("/productos/{id}")
    public ProductoDto detalle(@PathVariable Long id) {
        return productos.obtener(id);
    }

    /** Solo ADMIN (regla en SecurityConfig: /admin/**). */
    @PutMapping("/admin/productos/{id}")
    public ProductoDto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoUpdate r) {
        return productos.actualizar(id, r);
    }
}
