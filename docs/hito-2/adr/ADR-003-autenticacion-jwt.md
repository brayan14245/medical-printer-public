# ADR-003: Autenticación stateless con JWT, BCrypt y bloqueo por intentos

**Estado:** Aceptado
**Fecha:** 2026-11-25

## Contexto y problema

La SPA de React y la API están separadas, hay dos roles (ADMIN y USUARIO) y la cuenta debe protegerse de ataques de fuerza bruta (RNF-03).

## Alternativas evaluadas

1. Sesión de servidor con cookie.
2. OAuth2 con un proveedor externo.
3. JWT propio con contraseñas BCrypt.

## Decisión tomada

JWT firmado de 60 minutos con el rol en el token, validado por un filtro de Spring Security antes de los controladores. Contraseñas con BCrypt y bloqueo de 15 minutos tras 5 intentos fallidos (respuesta 429).

## Consecuencias

**Positivas**

- La API no guarda sesiones y se puede escalar horizontalmente.
- Los roles se aplican en un solo lugar y las rutas `/admin/**` responden 403 a quien no sea administrador.

**Negativas**

- Un token robado vale hasta que expira; no hay renovación ni revocación en esta versión.
- El frontend debe guardar el token y manejar el 401 para volver al login.
