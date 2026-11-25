package co.medicalprinter.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "consecutivos")
@Getter
@Setter
public class Consecutivo {
    @Id
    private String serie;
    private Long ultimoNumero;
}
