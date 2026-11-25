package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresas")
@Getter
@Setter
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String nit;
    private String telefono;
    private String correo;
    private String direccion;
    private String ciudad;
    private boolean activa = true;
    private LocalDateTime createdAt = LocalDateTime.now();
}
