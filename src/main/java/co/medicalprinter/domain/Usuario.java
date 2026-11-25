package co.medicalprinter.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;
    private String nombre;
    private String correo;
    private String passwordHash;
    private String telefono;
    @Enumerated(EnumType.STRING)
    private Rol rol = Rol.USUARIO;
    private boolean activo = true;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private LocalDateTime createdAt = LocalDateTime.now();
}
