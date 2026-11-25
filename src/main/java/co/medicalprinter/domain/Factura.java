package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facturas")
@Getter
@Setter
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    private Long numero;
    private BigDecimal subtotal;
    private BigDecimal impuestos;
    private BigDecimal total;
    @ManyToOne(optional = false)
    @JoinColumn(name = "emitida_por")
    private Usuario emitidaPor;
    private LocalDateTime fechaEmision = LocalDateTime.now();
}
