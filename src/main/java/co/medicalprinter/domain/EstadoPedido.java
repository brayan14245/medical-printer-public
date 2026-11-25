package co.medicalprinter.domain;

public enum EstadoPedido {
    PENDIENTE, CONFIRMADO, ENVIADO, ENTREGADO, CANCELADO;

    /** RN-07: transiciones permitidas. ENTREGADO y CANCELADO son finales. */
    public boolean puedePasarA(EstadoPedido destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADO || destino == CANCELADO;
            case CONFIRMADO -> destino == ENVIADO || destino == CANCELADO;
            case ENVIADO -> destino == ENTREGADO;
            default -> false;
        };
    }
}
