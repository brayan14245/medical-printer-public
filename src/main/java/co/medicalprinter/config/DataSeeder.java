package co.medicalprinter.config;

import co.medicalprinter.domain.*;
import co.medicalprinter.repo.*;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Datos de prueba: se crean solo si la base está vacía. */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PasswordEncoder encoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) return;

        Empresa clinica = new Empresa();
        clinica.setNombre("Clínica San José");
        clinica.setNit("900123456-7");
        clinica.setTelefono("3105550142");
        clinica.setCorreo("compras@clinicasanjose.co");
        clinica.setCiudad("Cúcuta");
        empresas.save(clinica);

        String adminPassword = environment.getProperty("APP_SEED_ADMIN_PASSWORD", "");
        if (!adminPassword.isBlank()) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setCorreo("admin@medicalprinter.co");
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setRol(Rol.ADMIN);
            usuarios.save(admin);
        }

        String userPassword = environment.getProperty("APP_SEED_USER_PASSWORD", "");
        if (!userPassword.isBlank()) {
            Usuario cliente = new Usuario();
            cliente.setNombre("Laura Martínez");
            cliente.setCorreo("laura@clinicasanjose.co");
            cliente.setPasswordHash(encoder.encode(userPassword));
            cliente.setEmpresa(clinica);
            usuarios.save(cliente);
        }

        Categoria monitoreo = categoria("Monitoreo");
        Categoria diagnostico = categoria("Diagnóstico");
        Categoria esterilizacion = categoria("Esterilización");
        Categoria mobiliario = categoria("Mobiliario");

        producto(monitoreo, "Monitor de signos vitales MX-500", "Pantalla de 12 pulgadas, ECG, SpO2 y presión no invasiva", "3200000", 8);
        producto(monitoreo, "Desfibrilador externo DEA", "Desfibrilador automático con guía de voz", "4800000", 3);
        producto(diagnostico, "Pulsioxímetro de dedo", "Medición de saturación de oxígeno y pulso", "180000", 12);
        producto(diagnostico, "Tensiómetro digital de brazo", "Medición automática de presión arterial", "150000", 20);
        producto(esterilizacion, "Autoclave 18 L", "Esterilizador a vapor de 18 litros", "5400000", 2);
        producto(mobiliario, "Camilla de transporte", "Camilla plegable con ruedas y barandas", "2100000", 5);

        log.info("Datos de prueba creados.");
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categorias.save(c);
    }

    private void producto(Categoria c, String nombre, String descripcion, String precio, int stock) {
        Producto p = new Producto();
        p.setCategoria(c);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        productos.save(p);
    }
}
