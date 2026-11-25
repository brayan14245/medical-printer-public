 Medical Printer: backend

Spring Boot 3 + Java 21 + PostgreSQL. Implementa HU-01 a HU-04 y el contrato de `openapi.yaml`.

Requisitos

- JDK 21 y Maven 3.9 (o la extensión Spring Boot de VS Code)
- PostgreSQL 16


## Configuración local

Configura estas variables de entorno antes de iniciar la aplicación:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` para PostgreSQL.
- `APP_JWT_SECRET` con una clave aleatoria de al menos 32 bytes.
- Opcionalmente, `APP_SEED_ADMIN_PASSWORD` y `APP_SEED_USER_PASSWORD` para crear las cuentas de prueba cuando se inicializan los datos en un perfil distinto de `prod`.

No guardes contraseñas ni claves reales en el repositorio. Los archivos `.env` y los datos de `uploads/` están excluidos de Git.

## Endpoints

| Método y ruta | Acceso |
| --- | --- |
| POST /auth/login, POST /auth/registro | Público |
| GET /productos, GET /productos/{id} | Público |
| GET /empresas?nit=, POST /empresas | Autenticado |
| POST /pedidos, GET /pedidos/mis | Autenticado |
| GET /admin/resumen, GET /admin/pedidos, GET /admin/facturas | ADMIN |
| PATCH /admin/pedidos/{id}/estado | ADMIN |
| POST /admin/pedidos/{id}/factura | ADMIN |
| PUT /admin/productos/{id} | ADMIN |

## Dónde vive cada requisito

| Requisito | Archivo |
| --- | --- |
| RN-01, RN-06 (sesión y rol) | `config/SecurityConfig` |
| RN-02, RNF-03 (registro, login, bloqueo) | `service/AuthService` |
| RN-03, RN-04, RN-05, RNF-02, RNF-05 | `service/PedidoService` |
| RN-07, RN-08 | `service/PedidoEstadoService`, `domain/EstadoPedido` |
| RN-09, RN-10, RNF-06 | `service/FacturaService` |
| RNF-04 (notificación asíncrona) | `service/NotificacionListener` |
| RNF-01 (catálogo paginado) | `service/ProductoService`, índice en `schema.sql` |

## Pruebas automatizadas

Ejecuta `mvn test`. Las pruebas siguen el patrón AAA (Arrange–Act–Assert) e incluyen:

- Pruebas unitarias de `PedidoService` con Mockito para creación, cálculo del total, stock insuficiente y productos inexistentes.
- Pruebas de integración HTTP para `PedidoController` con `@WebMvcTest` y MockMvc, incluyendo validación y mapeo de errores.
- Pruebas de repositorio con `@DataJpaTest` y H2 para paginación y filtro del catálogo.

Para compilar el frontend, ejecuta `npm run build` desde `frontend/`.

