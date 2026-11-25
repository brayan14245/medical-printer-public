package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedido_items")
@Getter
@Setter
public class PedidoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;
    private int cantidad;
    /** Precio congelado al crear el pedido (RN-10). */
    private BigDecimal precioUnitario;
}
