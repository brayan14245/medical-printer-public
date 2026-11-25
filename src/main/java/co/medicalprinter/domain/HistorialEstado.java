package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "historial_estados")
@Getter
@Setter
public class HistorialEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoAnterior;
    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoNuevo;
    @ManyToOne(optional = false)
    @JoinColumn(name = "cambiado_por")
    private Usuario cambiadoPor;
    private LocalDateTime fecha = LocalDateTime.now();
}
