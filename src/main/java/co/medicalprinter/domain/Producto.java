package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    private String nombre;
    @Column(columnDefinition = "text")
    private String descripcion;
    private String imagenUrl;
    private BigDecimal precio;
    private int stock;
    @Enumerated(EnumType.STRING)
    private EstadoProducto estado = EstadoProducto.DISPONIBLE;
    private LocalDateTime createdAt = LocalDateTime.now();
}
