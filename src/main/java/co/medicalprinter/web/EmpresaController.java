package co.medicalprinter.web;

import static co.medicalprinter.dto.Dtos.*;

import co.medicalprinter.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {
    private final EmpresaService empresas;

    @GetMapping
    public EmpresaDto buscar(@RequestParam String nit) {
        return empresas.buscarPorNit(nit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaDto crear(@Valid @RequestBody EmpresaRequest r) {
        return empresas.crear(r);
    }
}
