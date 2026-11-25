package co.medicalprinter.service;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.domain.Empresa;
import co.medicalprinter.repo.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpresaService {
    private final EmpresaRepository empresas;

    @Transactional(readOnly = true)
    public EmpresaDto buscarPorNit(String nit) {
        Empresa e = empresas.findByNit(normalizar(nit))
                .orElseThrow(() -> BusinessException.noEncontrado("No existe una empresa con ese NIT"));
        return aDto(e);
    }

    /** Una empresa se registra una sola vez, identificada por su NIT. */
    @Transactional
    public EmpresaDto crear(EmpresaRequest r) {
        String nit = normalizar(r.nit());
        if (empresas.existsByNit(nit)) throw nitDuplicado();
        Empresa e = new Empresa();
        e.setNombre(r.nombre().trim());
        e.setNit(nit);
        e.setTelefono(r.telefono());
        e.setCorreo(r.correo());
        e.setDireccion(r.direccion());
        e.setCiudad(r.ciudad());
        try {
            return aDto(empresas.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw nitDuplicado();
        }
    }

    private static String normalizar(String nit) {
        return nit.trim();
    }

    private static BusinessException nitDuplicado() {
        return new BusinessException(HttpStatus.CONFLICT, "NIT_DUPLICADO",
                "Ya existe una empresa registrada con ese NIT", "RN-04");
    }

    private static EmpresaDto aDto(Empresa e) {
        return new EmpresaDto(e.getId(), e.getNombre(), e.getNit());
    }
}
