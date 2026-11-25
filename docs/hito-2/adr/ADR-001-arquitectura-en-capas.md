# ADR-001: Arquitectura en capas sobre un monolito

**Estado:** Aceptado
**Fecha:** 2026-11-25

## Contexto y problema

El sistema tiene cuatro módulos (productos, pedidos, usuarios y ventas), un equipo de tres personas y requisitos fuertes de consistencia: no vender más unidades de las que existen (RNF-02) y no repetir números de factura (RNF-06).

## Alternativas evaluadas

1. Arquitectura hexagonal / Clean Architecture.
2. Microservicios por módulo.
3. Monolito en capas.

## Decisión tomada

Monolito en capas (`controller`, `service`, `repository`, `domain`, `dto`, `exception`) con un solo despliegue y una sola base de datos. Las dependencias van hacia abajo: controller → service → repository → domain.

## Consecuencias

**Positivas**

- Las transacciones de pedido y factura son locales y ACID, sin sagas.
- Menos código, menos infraestructura y pruebas más simples.
- Menor latencia: las llamadas entre capas no usan red.

**Negativas**

- Todo el sistema escala junto; un fallo no controlado afecta a todos los módulos.
- Exige disciplina para que las capas no se mezclen; se mitiga con la regla de dependencia hacia abajo.
