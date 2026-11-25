package co.medicalprinter.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;
    private final String regla;

    public BusinessException(HttpStatus status, String codigo, String mensaje, String regla) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.regla = regla;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
    public String getRegla() { return regla; }

    public static BusinessException noEncontrado(String mensaje) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, null);
    }
}
