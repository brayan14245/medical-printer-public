package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Producto;
import co.medicalprinter.repo.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {
    private final ProductoRepository productos;

    /** RNF-01: consulta paginada con DTO ligero. */
    @Transactional(readOnly = true)
    public ProductoPage listar(EstadoProducto estado, Long categoriaId, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "page debe ser mayor o igual a 0 y size entre 1 y 50", null);
        }
        EstadoProducto e = estado == null ? EstadoProducto.DISPONIBLE : estado;
        PageRequest pr = PageRequest.of(page, size, Sort.by("id"));
        Page<Producto> res = categoriaId == null
                ? productos.findByEstado(e, pr)
                : productos.findByEstadoAndCategoria_Id(e, categoriaId, pr);
        return new ProductoPage(res.map(ProductoService::aDto).getContent(),
                res.getNumber(), res.getSize(), res.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id) {
        return aDto(productos.findById(id)
                .orElseThrow(() -> BusinessException.noEncontrado("Producto no disponible")));
    }

    @Transactional
    public ProductoDto actualizar(Long id, ProductoUpdate r) {
        Producto p = productos.findById(id)
                .orElseThrow(() -> BusinessException.noEncontrado("El producto no existe"));
        p.setNombre(r.nombre().trim());
        p.setDescripcion(r.descripcion());
        p.setPrecio(r.precio());
        p.setStock(r.stock());
        p.setEstado(r.estado());
        return aDto(productos.save(p));
    }

    public static ProductoDto aDto(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getStock(),
                p.getEstado(), p.getImagenUrl(), p.getCategoria() == null ? null : p.getCategoria().getId());
    }
}
