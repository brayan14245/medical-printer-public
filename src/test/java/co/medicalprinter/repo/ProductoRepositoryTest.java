package co.medicalprinter.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.medicalprinter.domain.Categoria;
import co.medicalprinter.domain.EstadoProducto;
import co.medicalprinter.domain.Producto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medicalprinter-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductoRepositoryTest {
    @Autowired private ProductoRepository productos;
    @Autowired private CategoriaRepository categorias;

    @Test
    void paginaProductosDisponiblesSinIncluirLosAgotados() {
        // Arrange
        Categoria categoria = categoria("Monitoreo");
        productos.saveAndFlush(producto(categoria, "Monitor A", EstadoProducto.DISPONIBLE));
        productos.saveAndFlush(producto(categoria, "Monitor B", EstadoProducto.DISPONIBLE));
        productos.saveAndFlush(producto(categoria, "Monitor C", EstadoProducto.AGOTADO));

        // Act
        var pagina = productos.findByEstado(EstadoProducto.DISPONIBLE,
                PageRequest.of(0, 1, Sort.by("id")));

        // Assert
        assertEquals(1, pagina.getContent().size());
        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals("Monitor A", pagina.getContent().get(0).getNombre());
    }

    @Test
    void filtraProductosDisponiblesPorCategoria() {
        // Arrange
        Categoria monitoreo = categoria("Monitoreo");
        Categoria diagnostico = categoria("Diagnóstico");
        productos.saveAndFlush(producto(monitoreo, "Monitor", EstadoProducto.DISPONIBLE));
        productos.saveAndFlush(producto(diagnostico, "Pulsioxímetro", EstadoProducto.DISPONIBLE));

        // Act
        var resultado = productos.findByEstadoAndCategoria_Id(EstadoProducto.DISPONIBLE,
                diagnostico.getId(), PageRequest.of(0, 10));

        // Assert
        assertEquals(1, resultado.getTotalElements());
        assertEquals("Pulsioxímetro", resultado.getContent().get(0).getNombre());
    }

    private Categoria categoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categorias.saveAndFlush(categoria);
    }

    private static Producto producto(Categoria categoria, String nombre, EstadoProducto estado) {
        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setStock(3);
        producto.setEstado(estado);
        return producto;
    }
}
