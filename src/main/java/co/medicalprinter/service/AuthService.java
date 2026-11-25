package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.config.JwtService;
import co.medicalprinter.domain.Empresa;
import co.medicalprinter.domain.Usuario;
import co.medicalprinter.repo.EmpresaRepository;
import co.medicalprinter.repo.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /** HU-01 / RN-02. */
    @Transactional
    public AuthResponse registrar(RegistroRequest r) {
        String correo = r.correo().trim().toLowerCase();
        if (!PASSWORD.matcher(r.password()).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                    "La contraseña debe tener mínimo 8 caracteres con letras y números", "RN-02");
        }
        if (usuarios.existsByCorreo(correo)) throw correoDuplicado();

        Empresa empresa = null;
        if (r.empresaId() != null) {
            empresa = empresas.findById(r.empresaId())
                    .orElseThrow(() -> BusinessException.noEncontrado("La empresa indicada no existe"));
        }
        Usuario u = new Usuario();
        u.setNombre(r.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(r.telefono());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setEmpresa(empresa);
        try {
            usuarios.saveAndFlush(u); // la restricción UNIQUE cubre el registro simultáneo
        } catch (DataIntegrityViolationException e) {
            throw correoDuplicado();
        }
        return respuesta(u);
    }

    /** RNF-03: 5 intentos fallidos bloquean la cuenta 15 minutos. */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest r) {
        Usuario u = usuarios.findByCorreo(r.correo().trim().toLowerCase())
                .orElseThrow(AuthService::credenciales);
        LocalDateTime ahora = LocalDateTime.now();
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora)) throw bloqueada();

        if (!u.isActivo() || !encoder.matches(r.password(), u.getPasswordHash())) {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            if (u.getIntentosFallidos() >= MAX_INTENTOS) {
                u.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                u.setIntentosFallidos(0);
                usuarios.save(u);
                throw bloqueada();
            }
            usuarios.save(u);
            throw credenciales();
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        usuarios.save(u);
        return respuesta(u);
    }

    private AuthResponse respuesta(Usuario u) {
        return new AuthResponse(jwt.generar(u), jwt.getMinutos(),
                new UsuarioResumen(u.getId(), u.getNombre(), u.getRol()));
    }

    private static BusinessException credenciales() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                "Correo o contraseña incorrectos", null);
    }

    private static BusinessException bloqueada() {
        return new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "CUENTA_BLOQUEADA",
                "Intenta de nuevo en " + MINUTOS_BLOQUEO + " minutos", null);
    }

    private static BusinessException correoDuplicado() {
        return new BusinessException(HttpStatus.CONFLICT, "CORREO_DUPLICADO",
                "El correo ya está registrado, inicia sesión", "RN-02");
    }
}
