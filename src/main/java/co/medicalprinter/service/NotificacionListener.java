package co.medicalprinter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RNF-04: el aviso al cliente se envía después del commit, en otro hilo y con 3 reintentos.
 * Una caída del correo o de WhatsApp nunca revierte el pedido.
 */
@Component
@Slf4j
public class NotificacionListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void alCrearPedido(PedidoCreadoEvent e) {
        enviar(e);
    }

    @Recover
    public void agotados(Exception ex, PedidoCreadoEvent e) {
        log.error("No se pudo notificar el pedido #{} tras 3 intentos: {}", e.pedidoId(), ex.getMessage());
    }

    /** Punto de integración: aquí va el envío real por SMTP o por la API de WhatsApp. */
    private void enviar(PedidoCreadoEvent e) {
        log.info("Notificación simulada: pedido #{} de {} confirmado, contacto {}",
                e.pedidoId(), e.destinatario(), e.contacto());
    }
}
