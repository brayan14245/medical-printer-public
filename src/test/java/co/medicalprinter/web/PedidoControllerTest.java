package co.medicalprinter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.medicalprinter.common.BusinessException;
import co.medicalprinter.config.JwtService;
import co.medicalprinter.domain.EstadoPedido;
import co.medicalprinter.dto.Dtos.ItemDto;
import co.medicalprinter.dto.Dtos.PedidoDto;
import co.medicalprinter.dto.Dtos.PedidoRequest;
import co.medicalprinter.service.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerTest {
    private static final String SOLICITUD_VALIDA = """
            {
              "destinatarioTipo": "PERSONA",
              "contacto": "3105550142",
              "items": [{"productoId": 10, "cantidad": 2}]
            }
            """;

    @Autowired private MockMvc mvc;
    @MockBean private PedidoService pedidos;
    @MockBean private JwtService jwtService;

    @Test
    void creaPedidoYResponde201ConElDto() throws Exception {
        // Arrange
        when(pedidos.crear(isNull(), eq("test-key"), any(PedidoRequest.class)))
                .thenReturn(new PedidoDto(42L, EstadoPedido.PENDIENTE, new BigDecimal("25.00"),
                        LocalDateTime.of(2026, 11, 21, 12, 0), "Laura Martínez",
                        List.of(new ItemDto(10L, "Monitor clínico", 2, new BigDecimal("12.50")))));

        // Act / Assert
        mvc.perform(post("/pedidos")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").value(42))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(25.0))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));
    }

    @Test
    void rechazaSolicitudInvalidaAntesDeInvocarElServicio() throws Exception {
        // Arrange
        String solicitudSinItems = """
                {
                  "destinatarioTipo": "PERSONA",
                  "contacto": "3105550142",
                  "items": []
                }
                """;

        // Act / Assert
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudSinItems))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verify(pedidos, never()).crear(any(), any(), any());
    }

    @Test
    void mapeaExcepcionDeNegocioA409ConElFormatoEstandar() throws Exception {
        // Arrange
        when(pedidos.crear(isNull(), eq("test-key"), any(PedidoRequest.class)))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                        "Stock insuficiente", "RN-03"));

        // Act / Assert
        mvc.perform(post("/pedidos")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.regla").value("RN-03"));
    }
}
