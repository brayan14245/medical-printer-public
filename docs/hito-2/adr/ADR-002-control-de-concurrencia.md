# ADR-002: Control de concurrencia con bloqueo pesimista y @Version

**Estado:** Aceptado
**Fecha:** 2026-11-25

## Contexto y problema

Dos clientes pueden pedir la última unidad al mismo tiempo (RNF-02), dos administradores pueden facturar o cambiar el mismo pedido (RNF-06 y HU-03) y un doble clic puede duplicar un pedido (RNF-05).

## Alternativas evaluadas

1. Bloqueo optimista (`@Version`) también sobre productos.
2. Nivel de aislamiento SERIALIZABLE en toda la transacción.
3. Bloqueo pesimista en las filas críticas más `@Version` en pedidos.

## Decisión tomada

Bloqueo pesimista (`SELECT ... FOR UPDATE`) sobre los productos de un pedido, ordenados por id para evitar interbloqueos, y sobre la fila del consecutivo de facturas. `@Version` en pedidos para los cambios de estado concurrentes, y `Idempotency-Key` con restricción UNIQUE para los dobles envíos.

## Consecuencias

**Positivas**

- Garantiza 0 % de stock negativo y 0 huecos en la numeración de facturas.
- El perdedor recibe una respuesta clara (409) en lugar de reintentos silenciosos.

**Negativas**

- Las transacciones que compiten por el mismo producto se serializan, lo que baja el rendimiento en picos extremos.
- Hay que mantener las transacciones cortas y el orden de bloqueo consistente.
