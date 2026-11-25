# ADR-004: PostgreSQL como motor de base de datos

**Estado:** Aceptado
**Fecha:** 2026-11-25

## Contexto y problema

Los datos son relacionales (pedidos con ítems, facturas, historial) y se necesitan restricciones de integridad, bloqueos de fila y tipos numéricos exactos para el dinero.

## Alternativas evaluadas

1. MySQL.
2. MongoDB.
3. PostgreSQL.

## Decisión tomada

PostgreSQL 16 con tipos `NUMERIC(12,2)`, restricciones `CHECK`, `UNIQUE` e índices compuestos.

## Consecuencias

**Positivas**

- `CHECK (stock >= 0)` y `UNIQUE (pedido_id)` hacen cumplir las reglas aunque falle el código.
- `SELECT ... FOR UPDATE` y MVCC encajan con la estrategia de concurrencia del ADR-002.

**Negativas**

- Es un servicio más que operar y respaldar.
- El equipo debe conocer el SQL propio de PostgreSQL.
