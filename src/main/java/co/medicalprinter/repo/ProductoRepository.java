package co.medicalprinter.repo;

import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Producto;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Page<Producto> findByEstado(EstadoProducto estado, Pageable pageable);
    Page<Producto> findByEstadoAndCategoria_Id(EstadoProducto estado, Long categoriaId, Pageable pageable);
    long countByEstado(EstadoProducto estado);

    /** RNF-02: bloqueo pesimista, ordenado por id para evitar deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> bloquearPorIds(@Param("ids") Collection<Long> ids);
}
