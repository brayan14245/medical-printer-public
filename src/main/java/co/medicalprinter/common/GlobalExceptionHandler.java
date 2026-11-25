package co.medicalprinter.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> negocio(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getCodigo(), e.getMessage(), e.getRegla()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldErrors().stream().findFirst();
        String mensaje = error.map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Uno o más campos no son válidos");
        String regla = error.map(f -> f.getField().contains("cantidad") ? "RN-03" : null).orElse(null);
        return ResponseEntity.badRequest().body(new ErrorResponse("DATOS_INVALIDOS", mensaje, regla));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> formato(Exception e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("DATOS_INVALIDOS", "Uno o más campos no son válidos", null));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> concurrencia(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "CONFLICTO_CONCURRENCIA", "El pedido cambió mientras lo editabas, recárgalo", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "CONFLICTO_DATOS", "El registro ya existe o viola una restricción de datos", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                "ACCESO_DENEGADO", "Solo el administrador puede realizar esta acción", "RN-06"));
    }
}
